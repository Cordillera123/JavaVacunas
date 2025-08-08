package com.vacutrack.dao;

import com.vacutrack.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Clase base abstracta para todos los DAOs del sistema VACU-TRACK - VERSIÓN MEJORADA
 * Proporciona métodos comunes para operaciones de base de datos
 * Maneja conexiones, transacciones y conversiones básicas
 *
 * @author VACU-TRACK Team
 * @version 1.1 - Mejorada
 * @param <T> Tipo de entidad que maneja el DAO
 * @param <ID> Tipo del identificador de la entidad
 */
public abstract class BaseDAO<T, ID> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final DatabaseConfig databaseConfig;

    // Constantes para paginación
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Constructor que inicializa la configuración de base de datos
     */
    protected BaseDAO() {
        this.databaseConfig = DatabaseConfig.getInstance();
    }

    // Métodos abstractos que deben implementar las clases hijas

    /**
     * Obtiene el nombre de la tabla principal
     * @return nombre de la tabla
     */
    protected abstract String getTableName();

    /**
     * Obtiene el nombre de la columna de ID
     * @return nombre de la columna ID
     */
    protected abstract String getIdColumnName();

    /**
     * Mapea un ResultSet a una entidad
     * @param rs ResultSet con los datos
     * @return entidad mapeada
     * @throws SQLException si hay error en el mapeo
     */
    protected abstract T mapResultSetToEntity(ResultSet rs) throws SQLException;

    /**
     * Establece los parámetros para insertar una entidad
     * @param ps PreparedStatement
     * @param entity entidad a insertar
     * @throws SQLException si hay error al establecer parámetros
     */
    protected abstract void setInsertParameters(PreparedStatement ps, T entity) throws SQLException;

    /**
     * Establece los parámetros para actualizar una entidad
     * @param ps PreparedStatement
     * @param entity entidad a actualizar
     * @throws SQLException si hay error al establecer parámetros
     */
    protected abstract void setUpdateParameters(PreparedStatement ps, T entity) throws SQLException;

    // NUEVO: Métodos para determinar si es insert o update

    /**
     * Determina si una entidad es nueva (para decidir entre insert y update)
     * Por defecto, verifica si el ID es null
     * @param entity entidad a verificar
     * @return true si es nueva (insert), false si existe (update)
     */
    protected boolean isNew(T entity) {
        try {
            // Usar reflexión para obtener el ID de manera genérica
            var idField = entity.getClass().getDeclaredMethod("getId");
            idField.setAccessible(true);
            Object id = idField.invoke(entity);
            return id == null;
        } catch (Exception e) {
            logger.warn("No se pudo determinar si la entidad es nueva, asumiendo que sí: {}", e.getMessage());
            return true;
        }
    }

    // Métodos CRUD básicos - MEJORADOS

    /**
     * Busca una entidad por ID
     * @param id identificador de la entidad
     * @return Optional con la entidad encontrada
     */
    public Optional<T> findById(ID id) {
        if (id == null) {
            logger.debug("ID proporcionado es null");
            return Optional.empty();
        }

        String sql = "SELECT * FROM " + getTableName() + " WHERE " + getIdColumnName() + " = ?";

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setParameter(ps, 1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    T entity = mapResultSetToEntity(rs);
                    logger.debug("Entidad encontrada por ID {}: {}", id, entity.getClass().getSimpleName());
                    return Optional.of(entity);
                }
            }

        } catch (SQLException e) {
            logger.error("Error al buscar entidad por ID {}", id, e);
            throw new RuntimeException("Error al buscar entidad por ID", e);
        }

        logger.debug("No se encontró entidad con ID {}", id);
        return Optional.empty();
    }

    /**
     * NUEVO: Método de conveniencia que retorna null en lugar de Optional
     * @param id identificador de la entidad
     * @return entidad encontrada o null si no existe
     */
    public T getById(ID id) {
        return findById(id).orElse(null);
    }

    /**
     * Obtiene todas las entidades
     * @return lista de todas las entidades
     */
    public List<T> findAll() {
        String sql = "SELECT * FROM " + getTableName() + " ORDER BY " + getIdColumnName();

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<T> entities = new ArrayList<>();
            while (rs.next()) {
                entities.add(mapResultSetToEntity(rs));
            }

            logger.debug("Encontradas {} entidades de tipo {}", entities.size(), getTableName());
            return entities;

        } catch (SQLException e) {
            logger.error("Error al obtener todas las entidades", e);
            throw new RuntimeException("Error al obtener todas las entidades", e);
        }
    }

    /**
     * NUEVO: Obtiene entidades con paginación
     * @param page número de página (0-based)
     * @param size tamaño de página
     * @return lista paginada de entidades
     */
    public List<T> findAll(int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0 || size > MAX_PAGE_SIZE) size = DEFAULT_PAGE_SIZE;

        int offset = page * size;
        String sql = "SELECT * FROM " + getTableName() +
                " ORDER BY " + getIdColumnName() +
                " LIMIT ? OFFSET ?";

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, size);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<T> entities = new ArrayList<>();
                while (rs.next()) {
                    entities.add(mapResultSetToEntity(rs));
                }

                logger.debug("Encontradas {} entidades en página {} (tamaño {})",
                        entities.size(), page, size);
                return entities;
            }

        } catch (SQLException e) {
            logger.error("Error al obtener entidades paginadas", e);
            throw new RuntimeException("Error al obtener entidades paginadas", e);
        }
    }

    /**
     * MEJORADO: Inserta una nueva entidad
     * @param entity entidad a insertar
     * @return true si se insertó correctamente
     */
    public boolean insert(T entity) {
        if (entity == null) {
            logger.warn("No se puede insertar una entidad null");
            return false;
        }

        String sql = buildInsertSql();

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setInsertParameters(ps, entity);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                logger.warn("La inserción falló, no se afectaron filas");
                return false;
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    assignGeneratedId(entity, generatedKeys);
                }
            }

            logger.debug("Entidad insertada exitosamente: {}", entity.getClass().getSimpleName());
            return true;

        } catch (SQLException e) {
            logger.error("Error al insertar entidad", e);
            return false;
        }
    }

    /**
     * MEJORADO: Actualiza una entidad existente
     * @param entity entidad a actualizar
     * @return true si se actualizó correctamente
     */
    public boolean update(T entity) {
        if (entity == null) {
            logger.warn("No se puede actualizar una entidad null");
            return false;
        }

        String sql = buildUpdateSql();

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setUpdateParameters(ps, entity);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                logger.warn("La actualización falló, no se encontró la entidad");
                return false;
            }

            logger.debug("Entidad actualizada exitosamente: {}", entity.getClass().getSimpleName());
            return true;

        } catch (SQLException e) {
            logger.error("Error al actualizar entidad", e);
            return false;
        }
    }

    /**
     * NUEVO: Guarda una entidad (insert si es nueva, update si existe)
     * @param entity entidad a guardar
     * @return true si se guardó correctamente
     */
    public boolean save(T entity) {
        if (entity == null) {
            logger.warn("No se puede guardar una entidad null");
            return false;
        }

        try {
            if (isNew(entity)) {
                logger.debug("Insertando nueva entidad: {}", entity.getClass().getSimpleName());
                return insert(entity);
            } else {
                logger.debug("Actualizando entidad existente: {}", entity.getClass().getSimpleName());
                return update(entity);
            }
        } catch (Exception e) {
            logger.error("Error al guardar entidad", e);
            return false;
        }
    }

    /**
     * NUEVO: Guarda una entidad en una transacción
     * @param entity entidad a guardar
     * @return true si se guardó correctamente
     */
    public boolean saveInTransaction(T entity) {
        try {
            executeInTransaction(conn -> {
                if (!save(entity)) {
                    throw new SQLException("Error al guardar la entidad");
                }
            });
            return true;
        } catch (SQLException e) {
            logger.error("Error al guardar entidad en transacción", e);
            return false;
        }
    }

    /**
     * NUEVO: Guarda múltiples entidades en una transacción
     * @param entities lista de entidades a guardar
     * @return true si todas se guardaron correctamente
     */
    public boolean saveAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            logger.debug("Lista de entidades vacía o null");
            return true;
        }

        try {
            executeInTransaction(conn -> {
                for (T entity : entities) {
                    if (!save(entity)) {
                        throw new SQLException("Error al guardar una de las entidades");
                    }
                }
            });
            logger.debug("Guardadas {} entidades en transacción", entities.size());
            return true;
        } catch (SQLException e) {
            logger.error("Error al guardar múltiples entidades", e);
            return false;
        }
    }

    /**
     * Elimina una entidad por ID
     * @param id identificador de la entidad a eliminar
     * @return true si se eliminó, false si no existía
     */
    public boolean deleteById(ID id) {
        if (id == null) {
            logger.warn("No se puede eliminar con ID null");
            return false;
        }

        String sql = "DELETE FROM " + getTableName() + " WHERE " + getIdColumnName() + " = ?";

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setParameter(ps, 1, id);

            int affectedRows = ps.executeUpdate();
            boolean deleted = affectedRows > 0;

            if (deleted) {
                logger.debug("Entidad eliminada exitosamente con ID {}", id);
            } else {
                logger.debug("No se encontró entidad para eliminar con ID {}", id);
            }

            return deleted;

        } catch (SQLException e) {
            logger.error("Error al eliminar entidad con ID {}", id, e);
            return false;
        }
    }

    /**
     * NUEVO: Eliminación suave (soft delete) - marca como inactivo
     * Debe ser sobrescrito en DAOs que soporten soft delete
     * @param id identificador de la entidad
     * @return true si se marcó como inactivo
     */
    public boolean softDelete(ID id) {
        logger.warn("Soft delete no implementado para {}, usando delete normal", getTableName());
        return deleteById(id);
    }

    /**
     * Cuenta el número total de entidades
     * @return número total de entidades
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + getTableName();

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                long count = rs.getLong(1);
                logger.debug("Conteo de entidades en {}: {}", getTableName(), count);
                return count;
            }

            return 0;

        } catch (SQLException e) {
            logger.error("Error al contar entidades", e);
            throw new RuntimeException("Error al contar entidades", e);
        }
    }

    /**
     * Verifica si existe una entidad con el ID dado
     * @param id identificador a verificar
     * @return true si existe
     */
    public boolean existsById(ID id) {
        if (id == null) {
            return false;
        }

        String sql = "SELECT 1 FROM " + getTableName() + " WHERE " + getIdColumnName() + " = ? LIMIT 1";

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setParameter(ps, 1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            logger.error("Error al verificar existencia de entidad con ID {}", id, e);
            return false;
        }
    }

    // Métodos de utilidad para consultas personalizadas - MEJORADOS

    /**
     * Ejecuta una consulta personalizada que retorna una lista de entidades
     * @param sql consulta SQL
     * @param parameters parámetros de la consulta
     * @return lista de entidades
     */
    protected List<T> executeQuery(String sql, Object... parameters) {
        logQuery(sql, parameters);

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setParameters(ps, parameters);

            try (ResultSet rs = ps.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapResultSetToEntity(rs));
                }

                logger.debug("Consulta ejecutada, {} resultados obtenidos", results.size());
                return results;
            }

        } catch (SQLException e) {
            logger.error("Error al ejecutar consulta: {}", sql, e);
            throw new RuntimeException("Error al ejecutar consulta", e);
        }
    }

    /**
     * NUEVO: Ejecuta consulta con paginación
     * @param sql consulta SQL base (sin LIMIT)
     * @param page página (0-based)
     * @param size tamaño de página
     * @param parameters parámetros de la consulta
     * @return lista paginada de entidades
     */
    protected List<T> executeQueryPaginated(String sql, int page, int size, Object... parameters) {
        if (page < 0) page = 0;
        if (size <= 0 || size > MAX_PAGE_SIZE) size = DEFAULT_PAGE_SIZE;

        String paginatedSql = sql + " LIMIT ? OFFSET ?";
        Object[] allParams = new Object[parameters.length + 2];
        System.arraycopy(parameters, 0, allParams, 0, parameters.length);
        allParams[parameters.length] = size;
        allParams[parameters.length + 1] = page * size;

        return executeQuery(paginatedSql, allParams);
    }

    /**
     * Ejecuta una consulta que retorna una sola entidad
     * @param sql consulta SQL
     * @param parameters parámetros de la consulta
     * @return entidad encontrada o null
     */
    protected T executeQuerySingle(String sql, Object... parameters) {
        List<T> results = executeQuery(sql, parameters);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * NUEVO: Ejecuta consulta que retorna Optional
     * @param sql consulta SQL
     * @param parameters parámetros de la consulta
     * @return Optional con la entidad encontrada
     */
    protected Optional<T> executeQuerySingleOptional(String sql, Object... parameters) {
        return Optional.ofNullable(executeQuerySingle(sql, parameters));
    }

    /**
     * Ejecuta una actualización (INSERT, UPDATE, DELETE)
     * @param sql consulta SQL
     * @param parameters parámetros de la consulta
     * @return número de filas afectadas
     */
    protected int executeUpdate(String sql, Object... parameters) {
        logQuery(sql, parameters);

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setParameters(ps, parameters);

            int affectedRows = ps.executeUpdate();
            logger.debug("Actualización ejecutada, filas afectadas: {}", affectedRows);

            return affectedRows;

        } catch (SQLException e) {
            logger.error("Error al ejecutar actualización: {}", sql, e);
            throw new RuntimeException("Error al ejecutar actualización", e);
        }
    }

    /**
     * Ejecuta una consulta que retorna un valor escalar
     * @param sql consulta SQL
     * @param parameters parámetros de la consulta
     * @return valor del resultado
     */
    protected Object executeScalar(String sql, Object... parameters) {
        logQuery(sql, parameters);

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setParameters(ps, parameters);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Object result = rs.getObject(1);
                    logger.debug("Consulta escalar ejecutada, resultado: {}", result);
                    return result;
                }
                return null;
            }

        } catch (SQLException e) {
            logger.error("Error al ejecutar consulta escalar: {}", sql, e);
            throw new RuntimeException("Error al ejecutar consulta escalar", e);
        }
    }

    /**
     * NUEVO: Ejecuta múltiples actualizaciones en batch
     * @param sql consulta SQL
     * @param parametersList lista de parámetros para cada ejecución
     * @return array con número de filas afectadas por cada operación
     */
    protected int[] executeBatch(String sql, List<Object[]> parametersList) {
        if (parametersList == null || parametersList.isEmpty()) {
            return new int[0];
        }

        logQuery(sql + " [BATCH: " + parametersList.size() + " operaciones]");

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Object[] parameters : parametersList) {
                setParameters(ps, parameters);
                ps.addBatch();
            }

            int[] results = ps.executeBatch();
            logger.debug("Batch ejecutado, {} operaciones procesadas", results.length);

            return results;

        } catch (SQLException e) {
            logger.error("Error al ejecutar batch: {}", sql, e);
            throw new RuntimeException("Error al ejecutar batch", e);
        }
    }

    // Métodos auxiliares - MEJORADOS

    /**
     * Construye el SQL para INSERT (debe ser sobrescrito si es necesario)
     * @return SQL de INSERT
     */
    protected String buildInsertSql() {
        throw new UnsupportedOperationException("buildInsertSql() debe ser implementado en la clase hija");
    }

    /**
     * Construye el SQL para UPDATE (debe ser sobrescrito si es necesario)
     * @return SQL de UPDATE
     */
    protected String buildUpdateSql() {
        throw new UnsupportedOperationException("buildUpdateSql() debe ser implementado en la clase hija");
    }

    /**
     * Asigna el ID generado a la entidad (debe ser sobrescrito si es necesario)
     * @param entity entidad a la cual asignar el ID
     * @param generatedKeys ResultSet con las claves generadas
     * @throws SQLException si hay error
     */
    protected void assignGeneratedId(T entity, ResultSet generatedKeys) throws SQLException {
        // Implementación por defecto vacía, se sobrescribe en clases hijas si es necesario
    }

    /**
     * MEJORADO: Establece múltiples parámetros en un PreparedStatement con validación
     * @param ps PreparedStatement
     * @param parameters parámetros a establecer
     * @throws SQLException si hay error
     */
    protected void setParameters(PreparedStatement ps, Object... parameters) throws SQLException {
        if (parameters == null) {
            return;
        }

        for (int i = 0; i < parameters.length; i++) {
            setParameter(ps, i + 1, parameters[i]);
        }
    }

    /**
     * MEJORADO: Establece un parámetro en un PreparedStatement con manejo completo de tipos
     * @param ps PreparedStatement
     * @param index índice del parámetro (1-based)
     * @param value valor del parámetro
     * @throws SQLException si hay error
     */
    protected void setParameter(PreparedStatement ps, int index, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NULL);
        } else if (value instanceof String) {
            String strValue = (String) value;
            ps.setString(index, strValue.isEmpty() ? null : strValue);
        } else if (value instanceof Integer) {
            ps.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            ps.setLong(index, (Long) value);
        } else if (value instanceof Boolean) {
            ps.setBoolean(index, (Boolean) value);
        } else if (value instanceof Double) {
            ps.setDouble(index, (Double) value);
        } else if (value instanceof Float) {
            ps.setFloat(index, (Float) value);
        } else if (value instanceof LocalDate) {
            ps.setDate(index, Date.valueOf((LocalDate) value));
        } else if (value instanceof LocalDateTime) {
            ps.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
        } else if (value instanceof java.math.BigDecimal) {
            ps.setBigDecimal(index, (java.math.BigDecimal) value);
        } else if (value instanceof Date) {
            ps.setDate(index, (Date) value);
        } else if (value instanceof Timestamp) {
            ps.setTimestamp(index, (Timestamp) value);
        } else if (value instanceof byte[]) {
            ps.setBytes(index, (byte[]) value);
        } else {
            // Para tipos no manejados específicamente
            ps.setObject(index, value);
        }
    }

    // Métodos de utilidad para conversiones - SIN CAMBIOS

    /**
     * Convierte Timestamp a LocalDateTime manejando nulls
     * @param timestamp timestamp a convertir
     * @return LocalDateTime o null
     */
    protected LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    /**
     * Convierte Date a LocalDate manejando nulls
     * @param date date a convertir
     * @return LocalDate o null
     */
    protected LocalDate toLocalDate(Date date) {
        return date != null ? date.toLocalDate() : null;
    }

    /**
     * Obtiene un Integer del ResultSet manejando nulls
     * @param rs ResultSet
     * @param columnName nombre de la columna
     * @return Integer o null
     * @throws SQLException si hay error
     */
    protected Integer getInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    /**
     * Obtiene un Boolean del ResultSet manejando nulls
     * @param rs ResultSet
     * @param columnName nombre de la columna
     * @return Boolean o null
     * @throws SQLException si hay error
     */
    protected Boolean getBoolean(ResultSet rs, String columnName) throws SQLException {
        boolean value = rs.getBoolean(columnName);
        return rs.wasNull() ? null : value;
    }

    /**
     * Obtiene un String del ResultSet manejando nulls y trimming
     * @param rs ResultSet
     * @param columnName nombre de la columna
     * @return String trimmed o null
     * @throws SQLException si hay error
     */
    protected String getString(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value != null ? value.trim() : null;
    }

    /**
     * MEJORADO: Obtiene un String no vacío del ResultSet
     * @param rs ResultSet
     * @param columnName nombre de la columna
     * @return String trimmed o null si está vacío
     * @throws SQLException si hay error
     */
    protected String getStringNotEmpty(ResultSet rs, String columnName) throws SQLException {
        String value = getString(rs, columnName);
        return (value != null && !value.isEmpty()) ? value : null;
    }

    /**
     * Obtiene un BigDecimal del ResultSet manejando nulls
     * @param rs ResultSet
     * @param columnName nombre de la columna
     * @return BigDecimal o null
     * @throws SQLException si hay error
     */
    protected java.math.BigDecimal getBigDecimal(ResultSet rs, String columnName) throws SQLException {
        java.math.BigDecimal value = rs.getBigDecimal(columnName);
        return rs.wasNull() ? null : value;
    }

    /**
     * NUEVO: Obtiene un Long del ResultSet manejando nulls
     * @param rs ResultSet
     * @param columnName nombre de la columna
     * @return Long o null
     * @throws SQLException si hay error
     */
    protected Long getLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    /**
     * Ejecuta una operación en una transacción
     * @param operation operación a ejecutar
     * @throws SQLException si hay error en la transacción
     */
    protected void executeInTransaction(DatabaseConfig.OperacionTransaccional operation) throws SQLException {
        databaseConfig.ejecutarEnTransaccion(operation);
    }

    /**
     * MEJORADO: Método de utilidad para logging de consultas SQL con mejor formato
     * @param sql consulta SQL
     * @param parameters parámetros de la consulta
     */
    protected void logQuery(String sql, Object... parameters) {
        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Ejecutando SQL: ").append(sql.replaceAll("\\s+", " ").trim());
            if (parameters != null && parameters.length > 0) {
                sb.append(" | Parámetros: [");
                for (int i = 0; i < parameters.length; i++) {
                    if (i > 0) sb.append(", ");
                    Object param = parameters[i];
                    if (param == null) {
                        sb.append("null");
                    } else if (param instanceof String) {
                        sb.append("'").append(param).append("'");
                    } else {
                        sb.append(param);
                    }
                }
                sb.append("]");
            }
            logger.debug(sb.toString());
        }
    }

    /**
     * Cierra recursos de base de datos de forma segura
     * @param resources recursos a cerrar (Connection, Statement, ResultSet, etc.)
     */
    protected void closeResources(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    logger.warn("Error al cerrar recurso: {}", e.getMessage());
                }
            }
        }
    }

    // NUEVOS: Métodos de utilidad adicionales

    /**
     * Construye una cláusula WHERE dinámica basada en parámetros no nulos
     * @param conditions mapa de condiciones (columna -> valor)
     * @param parameters lista donde se añaden los valores para el PreparedStatement
     * @return cláusula WHERE construida (sin incluir la palabra WHERE)
     */
    protected String buildDynamicWhere(java.util.Map<String, Object> conditions, List<Object> parameters) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }

        StringBuilder where = new StringBuilder();
        boolean first = true;

        for (java.util.Map.Entry<String, Object> entry : conditions.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                if (!first) {
                    where.append(" AND ");
                }
                where.append(entry.getKey()).append(" = ?");
                parameters.add(value);
                first = false;
            }
        }

        return where.toString();
    }

    /**
     * Construye una cláusula LIKE dinámica para búsquedas de texto
     * @param column nombre de la columna
     * @param searchTerm término de búsqueda
     * @param parameters lista donde se añade el valor para el PreparedStatement
     * @return cláusula LIKE construida
     */
    protected String buildLikeCondition(String column, String searchTerm, List<Object> parameters) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return "";
        }

        parameters.add("%" + searchTerm.trim() + "%");
        return column + " LIKE ?";
    }

    /**
     * Valida que una página y tamaño sean válidos
     * @param page número de página
     * @param size tamaño de página
     * @return array con [página_validada, tamaño_validado]
     */
    protected int[] validatePagination(int page, int size) {
        int validPage = Math.max(0, page);
        int validSize = (size <= 0 || size > MAX_PAGE_SIZE) ? DEFAULT_PAGE_SIZE : size;
        return new int[]{validPage, validSize};
    }

    /**
     * Ejecuta una operación de conteo con condiciones dinámicas
     * @param whereClause cláusula WHERE (sin la palabra WHERE)
     * @param parameters parámetros para la consulta
     * @return número de registros que cumplen las condiciones
     */
    protected long countWithConditions(String whereClause, Object... parameters) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(getTableName());

        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }

        Object result = executeScalar(sql.toString(), parameters);
        return result instanceof Number ? ((Number) result).longValue() : 0;
    }

    /**
     * Verifica si existen registros que cumplan ciertas condiciones
     * @param whereClause cláusula WHERE (sin la palabra WHERE)
     * @param parameters parámetros para la consulta
     * @return true si existen registros
     */
    protected boolean existsWithConditions(String whereClause, Object... parameters) {
        StringBuilder sql = new StringBuilder("SELECT 1 FROM ").append(getTableName());

        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }

        sql.append(" LIMIT 1");

        Object result = executeScalar(sql.toString(), parameters);
        return result != null;
    }

    /**
     * Construye una consulta SELECT básica con posibles joins
     * @param selectColumns columnas a seleccionar
     * @param joins cláusulas JOIN adicionales
     * @return consulta SELECT construida
     */
    protected String buildSelectQuery(String selectColumns, String joins) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(selectColumns != null ? selectColumns : "*");
        sql.append(" FROM ").append(getTableName());

        if (joins != null && !joins.trim().isEmpty()) {
            sql.append(" ").append(joins);
        }

        return sql.toString();
    }

    /**
     * Sanitiza un término de búsqueda removiendo caracteres peligrosos
     * @param searchTerm término a sanitizar
     * @return término sanitizado
     */
    protected String sanitizeSearchTerm(String searchTerm) {
        if (searchTerm == null) {
            return null;
        }

        return searchTerm.trim()
                .replaceAll("[%_\\\\]", "\\\\$0") // Escapa caracteres especiales de LIKE
                .replaceAll("[';\"\\-\\-/\\*]", ""); // Remueve caracteres potencialmente peligrosos
    }

    /**
     * Construye un ORDER BY dinámico con validación de columnas permitidas
     * @param sortField campo por el que ordenar
     * @param sortDirection dirección del ordenamiento (ASC/DESC)
     * @param allowedFields campos permitidos para ordenar
     * @return cláusula ORDER BY construida
     */
    protected String buildOrderBy(String sortField, String sortDirection, String... allowedFields) {
        // Validar campo de ordenamiento
        if (sortField == null || sortField.trim().isEmpty()) {
            return "ORDER BY " + getIdColumnName();
        }

        // Verificar si el campo está en la lista de permitidos
        boolean fieldAllowed = false;
        if (allowedFields != null) {
            for (String allowed : allowedFields) {
                if (allowed.equals(sortField)) {
                    fieldAllowed = true;
                    break;
                }
            }
        } else {
            fieldAllowed = true; // Si no se especifican campos permitidos, permite cualquiera
        }

        if (!fieldAllowed) {
            logger.warn("Campo de ordenamiento no permitido: {}, usando ID por defecto", sortField);
            sortField = getIdColumnName();
        }

        // Validar dirección
        String direction = "ASC";
        if ("DESC".equalsIgnoreCase(sortDirection)) {
            direction = "DESC";
        }

        return "ORDER BY " + sortField + " " + direction;
    }

    /**
     * Método de utilidad para crear mensajes de error estandarizados
     * @param operation operación que falló
     * @param entityType tipo de entidad
     * @param identifier identificador (puede ser null)
     * @return mensaje de error formateado
     */
    protected String createErrorMessage(String operation, String entityType, Object identifier) {
        StringBuilder msg = new StringBuilder("Error al ");
        msg.append(operation).append(" ");
        msg.append(entityType != null ? entityType : "entidad");

        if (identifier != null) {
            msg.append(" con identificador: ").append(identifier);
        }

        return msg.toString();
    }

    /**
     * Método de utilidad para validar parámetros requeridos
     * @param paramName nombre del parámetro
     * @param paramValue valor del parámetro
     * @throws IllegalArgumentException si el parámetro es null o inválido
     */
    protected void requireNonNull(String paramName, Object paramValue) {
        if (paramValue == null) {
            throw new IllegalArgumentException(paramName + " no puede ser null");
        }
    }

    /**
     * Método de utilidad para validar strings no vacíos
     * @param paramName nombre del parámetro
     * @param paramValue valor del parámetro
     * @throws IllegalArgumentException si el string es null o vacío
     */
    protected void requireNonEmpty(String paramName, String paramValue) {
        if (paramValue == null || paramValue.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " no puede ser null o vacío");
        }
    }

    /**
     * Convierte un ResultSet completo a una lista de entidades
     * Útil para casos especiales donde se necesita procesar todo el ResultSet
     * @param rs ResultSet a procesar
     * @return lista de entidades
     * @throws SQLException si hay error en el procesamiento
     */
    protected List<T> mapResultSetToList(ResultSet rs) throws SQLException {
        List<T> entities = new ArrayList<>();
        while (rs.next()) {
            entities.add(mapResultSetToEntity(rs));
        }
        return entities;
    }

    /**
     * Ejecuta una consulta de agregación (SUM, AVG, MIN, MAX, etc.)
     * @param aggregateFunction función de agregación con columna (ej: "SUM(precio)")
     * @param whereClause cláusula WHERE opcional
     * @param parameters parámetros para la consulta
     * @return resultado de la agregación
     */
    protected Object executeAggregate(String aggregateFunction, String whereClause, Object... parameters) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(aggregateFunction);
        sql.append(" FROM ").append(getTableName());

        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }

        return executeScalar(sql.toString(), parameters);
    }

    /**
     * Método para obtener el valor siguiente de una secuencia (útil para algunos DBMS)
     * Por defecto retorna null, debe ser sobrescrito para DBMS que usen secuencias
     * @param sequenceName nombre de la secuencia
     * @return próximo valor de la secuencia o null si no aplica
     */
    protected Long getNextSequenceValue(String sequenceName) {
        // Implementación por defecto para MySQL/MariaDB que usan AUTO_INCREMENT
        // Para PostgreSQL, Oracle, etc., se debe sobrescribir este método
        return null;
    }

    /**
     * Valida el estado de la conexión a la base de datos
     * @return true si la conexión está activa y funcional
     */
    public boolean isConnectionHealthy() {
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {

            return rs.next() && rs.getInt(1) == 1;

        } catch (SQLException e) {
            logger.warn("Problema con la conexión a la base de datos", e);
            return false;
        }
    }

    /**
     * Obtiene información del esquema de la tabla
     * @return información básica del esquema de la tabla
     */
    public String getTableSchema() {
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_KEY " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? " +
                "ORDER BY ORDINAL_POSITION";

        StringBuilder schema = new StringBuilder();
        schema.append("Esquema de la tabla ").append(getTableName()).append(":\n");

        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, getTableName());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    schema.append("- ").append(rs.getString("COLUMN_NAME"))
                            .append(" (").append(rs.getString("DATA_TYPE")).append(")")
                            .append(" NULL: ").append(rs.getString("IS_NULLABLE"))
                            .append(" KEY: ").append(rs.getString("COLUMN_KEY"))
                            .append("\n");
                }
            }

        } catch (SQLException e) {
            logger.error("Error al obtener esquema de tabla", e);
            return "Error al obtener esquema: " + e.getMessage();
        }

        return schema.toString();
    }
}
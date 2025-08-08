package com.vacutrack.dao;

import com.vacutrack.model.TipoUsuario;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO para la entidad TipoUsuario
 * Maneja operaciones de base de datos relacionadas con tipos de usuario
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class TipoUsuarioDAO extends BaseDAO<TipoUsuario, Integer> {

    // Instancia singleton
    private static TipoUsuarioDAO instance;

    // SQL Queries
    private static final String INSERT_SQL =
            "INSERT INTO tipos_usuario (nombre, descripcion, activo, fecha_creacion) VALUES (?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE tipos_usuario SET nombre = ?, descripcion = ?, activo = ? WHERE id = ?";

    private static final String FIND_BY_NAME_SQL =
            "SELECT * FROM tipos_usuario WHERE nombre = ?";

    private static final String FIND_ACTIVE_SQL =
            "SELECT * FROM tipos_usuario WHERE activo = true ORDER BY nombre";

    private static final String FIND_BY_NAME_ACTIVE_SQL =
            "SELECT * FROM tipos_usuario WHERE nombre = ? AND activo = true";

    /**
     * Constructor privado para patrón singleton
     */
    private TipoUsuarioDAO() {
        super();
    }

    /**
     * Obtiene la instancia singleton del DAO
     * @return instancia de TipoUsuarioDAO
     */
    public static synchronized TipoUsuarioDAO getInstance() {
        if (instance == null) {
            instance = new TipoUsuarioDAO();
        }
        return instance;
    }

    @Override
    protected String getTableName() {
        return "tipos_usuario";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected TipoUsuario mapResultSetToEntity(ResultSet rs) throws SQLException {
        TipoUsuario tipoUsuario = new TipoUsuario();

        tipoUsuario.setId(getInteger(rs, "id"));
        tipoUsuario.setNombre(getString(rs, "nombre"));
        tipoUsuario.setDescripcion(getString(rs, "descripcion"));
        tipoUsuario.setActivo(getBoolean(rs, "activo"));
        tipoUsuario.setFechaCreacion(toLocalDateTime(rs.getTimestamp("fecha_creacion")));

        return tipoUsuario;
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, TipoUsuario tipoUsuario) throws SQLException {
        ps.setString(1, tipoUsuario.getNombre());
        ps.setString(2, tipoUsuario.getDescripcion());
        ps.setBoolean(3, tipoUsuario.getActivo() != null ? tipoUsuario.getActivo() : true);
        ps.setTimestamp(4, java.sql.Timestamp.valueOf(
                tipoUsuario.getFechaCreacion() != null ? tipoUsuario.getFechaCreacion() : LocalDateTime.now()));
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, TipoUsuario tipoUsuario) throws SQLException {
        ps.setString(1, tipoUsuario.getNombre());
        ps.setString(2, tipoUsuario.getDescripcion());
        ps.setBoolean(3, tipoUsuario.getActivo() != null ? tipoUsuario.getActivo() : true);
        ps.setInt(4, tipoUsuario.getId());
    }

    @Override
    protected String buildInsertSql() {
        return INSERT_SQL;
    }

    @Override
    protected String buildUpdateSql() {
        return UPDATE_SQL;
    }

    @Override
    protected void assignGeneratedId(TipoUsuario tipoUsuario, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            tipoUsuario.setId(generatedKeys.getInt(1));
        }
    }

    // Métodos específicos

    /**
     * Busca un tipo de usuario por nombre
     * @param nombre nombre del tipo de usuario
     * @return Optional con el tipo encontrado
     */
    public Optional<TipoUsuario> findByName(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return Optional.empty();
        }

        logQuery(FIND_BY_NAME_SQL, nombre);
        TipoUsuario tipoUsuario = executeQuerySingle(FIND_BY_NAME_SQL, nombre.trim().toUpperCase());

        if (tipoUsuario != null) {
            logger.debug("Tipo de usuario encontrado por nombre: {}", nombre);
        } else {
            logger.debug("No se encontró tipo de usuario con nombre: {}", nombre);
        }

        return Optional.ofNullable(tipoUsuario);
    }

    /**
     * Busca un tipo de usuario activo por nombre
     * @param nombre nombre del tipo de usuario
     * @return Optional con el tipo encontrado
     */
    public Optional<TipoUsuario> findActiveByName(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return Optional.empty();
        }

        logQuery(FIND_BY_NAME_ACTIVE_SQL, nombre);
        TipoUsuario tipoUsuario = executeQuerySingle(FIND_BY_NAME_ACTIVE_SQL, nombre.trim().toUpperCase());

        if (tipoUsuario != null) {
            logger.debug("Tipo de usuario activo encontrado por nombre: {}", nombre);
        } else {
            logger.debug("No se encontró tipo de usuario activo con nombre: {}", nombre);
        }

        return Optional.ofNullable(tipoUsuario);
    }

    /**
     * Obtiene todos los tipos de usuario activos
     * @return lista de tipos de usuario activos
     */
    public List<TipoUsuario> findActiveTypes() {
        logQuery(FIND_ACTIVE_SQL);
        List<TipoUsuario> tiposUsuario = executeQuery(FIND_ACTIVE_SQL);

        logger.debug("Encontrados {} tipos de usuario activos", tiposUsuario.size());
        return tiposUsuario;
    }

    /**
     * Busca el tipo de usuario PADRE_FAMILIA
     * @return Optional con el tipo Padre de Familia
     */
    public Optional<TipoUsuario> findPadreFamilia() {
        return findActiveByName(TipoUsuario.PADRE_FAMILIA);
    }

    /**
     * Busca el tipo de usuario PROFESIONAL_ENFERMERIA
     * @return Optional con el tipo Profesional de Enfermería
     */
    public Optional<TipoUsuario> findProfesionalEnfermeria() {
        return findActiveByName(TipoUsuario.PROFESIONAL_ENFERMERIA);
    }

    /**
     * Busca el tipo de usuario ADMINISTRADOR
     * @return Optional con el tipo Administrador
     */
    public Optional<TipoUsuario> findAdministrador() {
        return findActiveByName(TipoUsuario.ADMINISTRADOR);
    }

    /**
     * Verifica si existe un tipo de usuario con el nombre dado
     * @param nombre nombre a verificar
     * @return true si existe
     */
    public boolean existsByName(String nombre) {
        return findByName(nombre).isPresent();
    }

    /**
     * Verifica si existe un tipo de usuario con el nombre dado excluyendo un ID específico
     * @param nombre nombre a verificar
     * @param excludeId ID a excluir de la verificación
     * @return true si existe en otro registro
     */
    public boolean existsByNameExcluding(String nombre, Integer excludeId) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM tipos_usuario WHERE nombre = ? AND id != ?";
        logQuery(sql, nombre, excludeId);

        Object result = executeScalar(sql, nombre.trim().toUpperCase(), excludeId);
        return result instanceof Number && ((Number) result).longValue() > 0;
    }

    /**
     * Inicializa los tipos de usuario por defecto si no existen
     */
    public void initializeDefaultTypes() {
        logger.info("Inicializando tipos de usuario por defecto...");

        try {
            // Verificar y crear Padre de Familia
            if (!existsByName(TipoUsuario.PADRE_FAMILIA)) {
                TipoUsuario padreFamilia = TipoUsuario.crearPadreFamilia();
                insert(padreFamilia);
                logger.info("Tipo de usuario '{}' creado", TipoUsuario.PADRE_FAMILIA);
            }

            // Verificar y crear Profesional de Enfermería
            if (!existsByName(TipoUsuario.PROFESIONAL_ENFERMERIA)) {
                TipoUsuario profesional = TipoUsuario.crearProfesionalEnfermeria();
                insert(profesional);
                logger.info("Tipo de usuario '{}' creado", TipoUsuario.PROFESIONAL_ENFERMERIA);
            }

            // Verificar y crear Administrador
            if (!existsByName(TipoUsuario.ADMINISTRADOR)) {
                TipoUsuario administrador = TipoUsuario.crearAdministrador();
                insert(administrador);
                logger.info("Tipo de usuario '{}' creado", TipoUsuario.ADMINISTRADOR);
            }

            logger.info("Inicialización de tipos de usuario completada");

        } catch (Exception e) {
            logger.error("Error al inicializar tipos de usuario por defecto", e);
            throw new RuntimeException("Error al inicializar tipos de usuario", e);
        }
    }

    /**
     * Desactiva un tipo de usuario (no se puede eliminar porque puede tener usuarios asociados)
     * @param id ID del tipo de usuario
     * @return true si se desactivó correctamente
     */
    public boolean deactivateType(Integer id) {
        if (id == null) {
            return false;
        }

        // Verificar que no sea un tipo de sistema crítico
        Optional<TipoUsuario> tipoOpt = Optional.ofNullable(findById(id));
        if (tipoOpt.isPresent()) {
            String nombre = tipoOpt.get().getNombre();
            if (TipoUsuario.ADMINISTRADOR.equals(nombre)) {
                logger.warn("No se puede desactivar el tipo de usuario ADMINISTRADOR");
                return false;
            }
        }

        String sql = "UPDATE tipos_usuario SET activo = false WHERE id = ?";

        try {
            int affectedRows = executeUpdate(sql, id);
            boolean deactivated = affectedRows > 0;

            if (deactivated) {
                logger.info("Tipo de usuario desactivado ID: {}", id);
            } else {
                logger.warn("No se pudo desactivar el tipo de usuario ID: {}", id);
            }

            return deactivated;

        } catch (Exception e) {
            logger.error("Error al desactivar tipo de usuario ID: {}", id, e);
            return false;
        }
    }

    /**
     * Activa un tipo de usuario
     * @param id ID del tipo de usuario
     * @return true si se activó correctamente
     */
    public boolean activateType(Integer id) {
        if (id == null) {
            return false;
        }

        String sql = "UPDATE tipos_usuario SET activo = true WHERE id = ?";

        try {
            int affectedRows = executeUpdate(sql, id);
            boolean activated = affectedRows > 0;

            if (activated) {
                logger.info("Tipo de usuario activado ID: {}", id);
            } else {
                logger.warn("No se pudo activar el tipo de usuario ID: {}", id);
            }

            return activated;

        } catch (Exception e) {
            logger.error("Error al activar tipo de usuario ID: {}", id, e);
            return false;
        }
    }

    /**
     * Obtiene estadísticas de uso de tipos de usuario
     * @return String con estadísticas
     */
    public String getUsageStatistics() {
        String sql = "SELECT tu.nombre, tu.descripcion, COUNT(u.id) as total_usuarios " +
                "FROM tipos_usuario tu " +
                "LEFT JOIN usuarios u ON tu.id = u.tipo_usuario_id AND u.activo = true " +
                "WHERE tu.activo = true " +
                "GROUP BY tu.id, tu.nombre, tu.descripcion " +
                "ORDER BY total_usuarios DESC";

        StringBuilder stats = new StringBuilder();
        stats.append("Estadísticas de Tipos de Usuario:\n");
        stats.append("================================\n");

        try (var conn = databaseConfig.getConnection();
             var ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {

            while (rs.next()) {
                String nombre = rs.getString("nombre");
                String descripcion = rs.getString("descripcion");
                int totalUsuarios = rs.getInt("total_usuarios");

                stats.append(String.format("- %s: %d usuarios\n", descripcion, totalUsuarios));
            }

        } catch (SQLException e) {
            logger.error("Error al obtener estadísticas de uso", e);
            stats.append("Error al obtener estadísticas");
        }

        return stats.toString();
    }
}
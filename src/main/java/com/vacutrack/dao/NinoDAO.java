package com.vacutrack.dao;

import com.vacutrack.model.Nino;
import com.vacutrack.model.PadreFamilia;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO para la entidad Nino - VERSIÓN CORREGIDA
 * Maneja operaciones de base de datos relacionadas con niños registrados
 * Incluye métodos específicos para búsquedas por edad, padre, etc.
 *
 * @author VACU-TRACK Team
 * @version 1.1 - Corregida
 */
public class NinoDAO extends BaseDAO<Nino, Integer> {

    // Instancia singleton
    private static NinoDAO instance;

    // SQL Queries - CORREGIDAS
    private static final String INSERT_SQL =
            "INSERT INTO ninos (padre_id, nombres, apellidos, cedula, fecha_nacimiento, sexo, " +
                    "lugar_nacimiento, peso_nacimiento, talla_nacimiento, activo, fecha_registro) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE ninos SET padre_id = ?, nombres = ?, apellidos = ?, cedula = ?, " +
                    "fecha_nacimiento = ?, sexo = ?, lugar_nacimiento = ?, peso_nacimiento = ?, " +
                    "talla_nacimiento = ?, activo = ? WHERE id = ?";

    private static final String FIND_BY_PADRE_SQL =
            "SELECT n.*, pf.nombres as padre_nombres, pf.apellidos as padre_apellidos " +
                    "FROM ninos n " +
                    "LEFT JOIN padres_familia pf ON n.padre_id = pf.id " +
                    "WHERE n.padre_id = ? AND n.activo = true " +
                    "ORDER BY n.fecha_nacimiento ASC";

    private static final String FIND_BY_CEDULA_SQL =
            "SELECT n.*, pf.nombres as padre_nombres, pf.apellidos as padre_apellidos " +
                    "FROM ninos n " +
                    "LEFT JOIN padres_familia pf ON n.padre_id = pf.id " +
                    "WHERE n.cedula = ? AND n.activo = true";

    private static final String FIND_ACTIVE_SQL =
            "SELECT n.*, pf.nombres as padre_nombres, pf.apellidos as padre_apellidos " +
                    "FROM ninos n " +
                    "LEFT JOIN padres_familia pf ON n.padre_id = pf.id " +
                    "WHERE n.activo = true " +
                    "ORDER BY n.fecha_nacimiento DESC";

    // CORREGIDA: Query de rango de edad con lógica correcta
    private static final String FIND_BY_AGE_RANGE_SQL =
            "SELECT n.*, pf.nombres as padre_nombres, pf.apellidos as padre_apellidos " +
                    "FROM ninos n " +
                    "LEFT JOIN padres_familia pf ON n.padre_id = pf.id " +
                    "WHERE n.activo = true " +
                    "AND DATEDIFF(CURDATE(), n.fecha_nacimiento) BETWEEN ? AND ? " +
                    "ORDER BY n.fecha_nacimiento DESC";

    private static final String SEARCH_BY_NAME_SQL =
            "SELECT n.*, pf.nombres as padre_nombres, pf.apellidos as padre_apellidos " +
                    "FROM ninos n " +
                    "LEFT JOIN padres_familia pf ON n.padre_id = pf.id " +
                    "WHERE n.activo = true " +
                    "AND (UPPER(n.nombres) LIKE UPPER(?) OR UPPER(n.apellidos) LIKE UPPER(?) " +
                    "OR UPPER(CONCAT(n.nombres, ' ', n.apellidos)) LIKE UPPER(?)) " +
                    "ORDER BY n.nombres, n.apellidos";

    private static final String COUNT_BY_PADRE_SQL =
            "SELECT COUNT(*) FROM ninos WHERE padre_id = ? AND activo = true";

    private static final String FIND_RECENT_BIRTHS_SQL =
            "SELECT n.*, pf.nombres as padre_nombres, pf.apellidos as padre_apellidos " +
                    "FROM ninos n " +
                    "LEFT JOIN padres_familia pf ON n.padre_id = pf.id " +
                    "WHERE n.activo = true AND n.fecha_nacimiento >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                    "ORDER BY n.fecha_nacimiento DESC";

    // NUEVA: Query para verificar cédula única
    private static final String EXISTS_BY_CEDULA_SQL =
            "SELECT COUNT(*) FROM ninos WHERE cedula = ? AND activo = true";

    private static final String EXISTS_BY_CEDULA_EXCLUDING_SQL =
            "SELECT COUNT(*) FROM ninos WHERE cedula = ? AND id != ? AND activo = true";

    /**
     * Constructor privado para patrón singleton
     */
    private NinoDAO() {
        super();
    }

    /**
     * Obtiene la instancia singleton del DAO
     * @return instancia de NinoDAO
     */
    public static synchronized NinoDAO getInstance() {
        if (instance == null) {
            instance = new NinoDAO();
        }
        return instance;
    }

    @Override
    protected String getTableName() {
        return "ninos";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected Nino mapResultSetToEntity(ResultSet rs) throws SQLException {
        Nino nino = new Nino();

        // Datos básicos del niño
        nino.setId(getInteger(rs, "id"));
        nino.setPadreId(getInteger(rs, "padre_id"));
        nino.setNombres(getString(rs, "nombres"));
        nino.setApellidos(getString(rs, "apellidos"));
        nino.setCedula(getString(rs, "cedula"));
        nino.setFechaNacimiento(toLocalDate(rs.getDate("fecha_nacimiento")));

        // CORREGIDA: Validación del campo sexo
        String sexo = getString(rs, "sexo");
        if ("M".equals(sexo) || "F".equals(sexo)) {
            nino.setSexo(sexo);
        }

        nino.setLugarNacimiento(getString(rs, "lugar_nacimiento"));
        nino.setPesoNacimiento(rs.getBigDecimal("peso_nacimiento"));
        nino.setTallaNacimiento(rs.getBigDecimal("talla_nacimiento"));
        nino.setActivo(getBoolean(rs, "activo"));
        nino.setFechaRegistro(toLocalDateTime(rs.getTimestamp("fecha_registro")));

        // Mapear información básica del padre si está disponible
        try {
            String padreNombres = getString(rs, "padre_nombres");
            String padreApellidos = getString(rs, "padre_apellidos");

            if (padreNombres != null && padreApellidos != null) {
                PadreFamilia padre = new PadreFamilia();
                padre.setId(nino.getPadreId());
                padre.setNombres(padreNombres);
                padre.setApellidos(padreApellidos);
                nino.setPadre(padre);
            }
        } catch (SQLException e) {
            // Información del padre no disponible en esta consulta, ignorar
            logger.debug("Información del padre no disponible en el ResultSet");
        }

        return nino;
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Nino nino) throws SQLException {
        ps.setInt(1, nino.getPadreId());
        ps.setString(2, nino.getNombres());
        ps.setString(3, nino.getApellidos());
        ps.setString(4, nino.getCedula());
        ps.setDate(5, java.sql.Date.valueOf(nino.getFechaNacimiento()));
        ps.setString(6, nino.getSexo());
        ps.setString(7, nino.getLugarNacimiento());

        if (nino.getPesoNacimiento() != null) {
            ps.setBigDecimal(8, nino.getPesoNacimiento());
        } else {
            ps.setNull(8, java.sql.Types.DECIMAL);
        }

        if (nino.getTallaNacimiento() != null) {
            ps.setBigDecimal(9, nino.getTallaNacimiento());
        } else {
            ps.setNull(9, java.sql.Types.DECIMAL);
        }

        ps.setBoolean(10, nino.getActivo() != null ? nino.getActivo() : true);
        ps.setTimestamp(11, java.sql.Timestamp.valueOf(
                nino.getFechaRegistro() != null ? nino.getFechaRegistro() : LocalDateTime.now()));
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, Nino nino) throws SQLException {
        ps.setInt(1, nino.getPadreId());
        ps.setString(2, nino.getNombres());
        ps.setString(3, nino.getApellidos());
        ps.setString(4, nino.getCedula());
        ps.setDate(5, java.sql.Date.valueOf(nino.getFechaNacimiento()));
        ps.setString(6, nino.getSexo());
        ps.setString(7, nino.getLugarNacimiento());

        if (nino.getPesoNacimiento() != null) {
            ps.setBigDecimal(8, nino.getPesoNacimiento());
        } else {
            ps.setNull(8, java.sql.Types.DECIMAL);
        }

        if (nino.getTallaNacimiento() != null) {
            ps.setBigDecimal(9, nino.getTallaNacimiento());
        } else {
            ps.setNull(9, java.sql.Types.DECIMAL);
        }

        ps.setBoolean(10, nino.getActivo() != null ? nino.getActivo() : true);
        ps.setInt(11, nino.getId());
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
    protected void assignGeneratedId(Nino nino, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            nino.setId(generatedKeys.getInt(1));
        }
    }

    // Métodos específicos - CORREGIDOS

    /**
     * Busca niños por ID del padre
     * @param padreId ID del padre de familia
     * @return lista de niños del padre
     */
    public List<Nino> findByPadre(Integer padreId) {
        if (padreId == null) {
            return List.of();
        }

        logQuery(FIND_BY_PADRE_SQL, padreId);
        List<Nino> ninos = executeQuery(FIND_BY_PADRE_SQL, padreId);

        logger.debug("Encontrados {} niños para padre ID: {}", ninos.size(), padreId);
        return ninos;
    }

    /**
     * Busca un niño por cédula
     * @param cedula cédula del niño
     * @return Optional con el niño encontrado
     */
    public Optional<Nino> findByCedula(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            return Optional.empty();
        }

        // Limpiar cédula antes de buscar
        String cedulaLimpia = cedula.trim().replaceAll("[^0-9]", "");
        if (cedulaLimpia.length() != 10) {
            return Optional.empty();
        }

        logQuery(FIND_BY_CEDULA_SQL, cedulaLimpia);
        Nino nino = executeQuerySingle(FIND_BY_CEDULA_SQL, cedulaLimpia);

        if (nino != null) {
            logger.debug("Niño encontrado por cédula: {}", cedulaLimpia);
        } else {
            logger.debug("No se encontró niño con cédula: {}", cedulaLimpia);
        }

        return Optional.ofNullable(nino);
    }

    /**
     * Obtiene todos los niños activos
     * @return lista de niños activos
     */
    public List<Nino> findActiveNinos() {
        logQuery(FIND_ACTIVE_SQL);
        List<Nino> ninos = executeQuery(FIND_ACTIVE_SQL);

        logger.debug("Encontrados {} niños activos", ninos.size());
        return ninos;
    }

    /**
     * CORREGIDA: Busca niños por rango de edad en días
     * @param edadMinimaDias edad mínima en días
     * @param edadMaximaDias edad máxima en días
     * @return lista de niños en el rango de edad
     */
    public List<Nino> findByAgeRange(int edadMinimaDias, int edadMaximaDias) {
        // Validar parámetros
        if (edadMinimaDias < 0 || edadMaximaDias < 0 || edadMinimaDias > edadMaximaDias) {
            logger.warn("Parámetros de edad inválidos: min={}, max={}", edadMinimaDias, edadMaximaDias);
            return List.of();
        }

        logQuery(FIND_BY_AGE_RANGE_SQL, edadMinimaDias, edadMaximaDias);
        List<Nino> ninos = executeQuery(FIND_BY_AGE_RANGE_SQL, edadMinimaDias, edadMaximaDias);

        logger.debug("Encontrados {} niños con edad entre {} y {} días",
                ninos.size(), edadMinimaDias, edadMaximaDias);
        return ninos;
    }

    /**
     * Busca niños por nombre (búsqueda parcial)
     * @param termino término de búsqueda
     * @return lista de niños que coinciden con el término
     */
    public List<Nino> searchByName(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            return List.of();
        }

        String searchTerm = "%" + termino.trim() + "%";
        logQuery(SEARCH_BY_NAME_SQL, searchTerm, searchTerm, searchTerm);
        List<Nino> ninos = executeQuery(SEARCH_BY_NAME_SQL, searchTerm, searchTerm, searchTerm);

        logger.debug("Encontrados {} niños con término de búsqueda: {}", ninos.size(), termino);
        return ninos;
    }

    /**
     * Cuenta niños por padre
     * @param padreId ID del padre
     * @return número de niños del padre
     */
    public long countByPadre(Integer padreId) {
        if (padreId == null) {
            return 0;
        }

        logQuery(COUNT_BY_PADRE_SQL, padreId);
        Object result = executeScalar(COUNT_BY_PADRE_SQL, padreId);

        long count = result instanceof Number ? ((Number) result).longValue() : 0;
        logger.debug("Conteo de niños para padre ID {}: {}", padreId, count);

        return count;
    }

    /**
     * Busca niños nacidos recientemente
     * @param days número de días hacia atrás
     * @return lista de niños nacidos en los últimos días
     */
    public List<Nino> findRecentBirths(int days) {
        if (days < 0) {
            return List.of();
        }

        logQuery(FIND_RECENT_BIRTHS_SQL, days);
        List<Nino> ninos = executeQuery(FIND_RECENT_BIRTHS_SQL, days);

        logger.debug("Encontrados {} niños nacidos en los últimos {} días", ninos.size(), days);
        return ninos;
    }

    /**
     * Busca recién nacidos (menos de 30 días)
     * @return lista de recién nacidos
     */
    public List<Nino> findRecienNacidos() {
        return findRecentBirths(30);
    }

    /**
     * Busca bebés (menos de 1 año)
     * @return lista de bebés
     */
    public List<Nino> findBebes() {
        return findByAgeRange(0, 365);
    }

    /**
     * Busca niños en edad de vacunación (0-18 años)
     * @return lista de niños en edad de vacunación
     */
    public List<Nino> findNinosVacunacion() {
        return findByAgeRange(0, 365 * 18);
    }

    /**
     * CORREGIDA: Verifica si una cédula ya está registrada
     * @param cedula cédula a verificar
     * @return true si ya existe
     */
    public boolean existsByCedula(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            return false;
        }

        // Limpiar cédula
        String cedulaLimpia = cedula.trim().replaceAll("[^0-9]", "");
        if (cedulaLimpia.length() != 10) {
            return false;
        }

        logQuery(EXISTS_BY_CEDULA_SQL, cedulaLimpia);
        Object result = executeScalar(EXISTS_BY_CEDULA_SQL, cedulaLimpia);
        return result instanceof Number && ((Number) result).longValue() > 0;
    }

    /**
     * CORREGIDA: Verifica si una cédula ya está registrada excluyendo un niño específico
     * @param cedula cédula a verificar
     * @param excludeNinoId ID del niño a excluir
     * @return true si ya existe en otro niño
     */
    public boolean existsByCedulaExcluding(String cedula, Integer excludeNinoId) {
        if (cedula == null || cedula.trim().isEmpty() || excludeNinoId == null) {
            return false;
        }

        // Limpiar cédula
        String cedulaLimpia = cedula.trim().replaceAll("[^0-9]", "");
        if (cedulaLimpia.length() != 10) {
            return false;
        }

        logQuery(EXISTS_BY_CEDULA_EXCLUDING_SQL, cedulaLimpia, excludeNinoId);
        Object result = executeScalar(EXISTS_BY_CEDULA_EXCLUDING_SQL, cedulaLimpia, excludeNinoId);
        return result instanceof Number && ((Number) result).longValue() > 0;
    }

    /**
     * Desactiva un niño (soft delete)
     * @param ninoId ID del niño a desactivar
     * @return true si se desactivó correctamente
     */
    public boolean deactivateNino(Integer ninoId) {
        if (ninoId == null) {
            return false;
        }

        String sql = "UPDATE ninos SET activo = false WHERE id = ?";

        try {
            int affectedRows = executeUpdate(sql, ninoId);
            boolean deactivated = affectedRows > 0;

            if (deactivated) {
                logger.info("Niño desactivado ID: {}", ninoId);
            } else {
                logger.warn("No se pudo desactivar el niño ID: {}", ninoId);
            }

            return deactivated;

        } catch (Exception e) {
            logger.error("Error al desactivar niño ID: {}", ninoId, e);
            return false;
        }
    }

    /**
     * Activa un niño
     * @param ninoId ID del niño a activar
     * @return true si se activó correctamente
     */
    public boolean activateNino(Integer ninoId) {
        if (ninoId == null) {
            return false;
        }

        String sql = "UPDATE ninos SET activo = true WHERE id = ?";

        try {
            int affectedRows = executeUpdate(sql, ninoId);
            boolean activated = affectedRows > 0;

            if (activated) {
                logger.info("Niño activado ID: {}", ninoId);
            } else {
                logger.warn("No se pudo activar el niño ID: {}", ninoId);
            }

            return activated;

        } catch (Exception e) {
            logger.error("Error al activar niño ID: {}", ninoId, e);
            return false;
        }
    }

    /**
     * NUEVA: Valida si los datos del niño son consistentes antes de guardar
     * @param nino niño a validar
     * @return true si es válido para guardar
     */
    public boolean validateForSave(Nino nino) {
        if (nino == null) {
            return false;
        }

        // Validar datos básicos
        if (!nino.esValido()) {
            logger.warn("Datos del niño no válidos: {}", nino.obtenerMensajesValidacion());
            return false;
        }

        // Validar unicidad de cédula si existe
        if (nino.getCedula() != null) {
            if (nino.getId() == null) {
                // Nuevo registro
                if (existsByCedula(nino.getCedula())) {
                    logger.warn("La cédula {} ya está registrada", nino.getCedula());
                    return false;
                }
            } else {
                // Actualización
                if (existsByCedulaExcluding(nino.getCedula(), nino.getId())) {
                    logger.warn("La cédula {} ya está registrada en otro niño", nino.getCedula());
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * SOBRESCRITA: Save con validación
     */
    @Override
    public boolean save(Nino nino) {
        if (!validateForSave(nino)) {
            return false;
        }
        return super.save(nino);
    }

    /**
     * SOBRESCRITA: Update con validación
     */
    @Override
    public boolean update(Nino nino) {
        if (!validateForSave(nino)) {
            return false;
        }
        return super.update(nino);
    }

    // Métodos de estadísticas se mantienen igual...
    // [Los métodos getGenderStatistics() y getAgeGroupStatistics() permanecen sin cambios]
}
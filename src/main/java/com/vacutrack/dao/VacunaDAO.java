package com.vacutrack.dao;

import com.vacutrack.model.Vacuna;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO para la entidad Vacuna
 * Maneja operaciones de base de datos relacionadas con el catálogo de vacunas
 * Incluye métodos específicos para el esquema ecuatoriano de vacunación
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class VacunaDAO extends BaseDAO<Vacuna, Integer> {

    // Instancia singleton
    private static VacunaDAO instance;

    // SQL Queries
    private static final String INSERT_SQL =
            "INSERT INTO vacunas (codigo, nombre, nombre_comercial, descripcion, fabricante, " +
                    "dosis_total, via_administracion, sitio_aplicacion, contraindicaciones, efectos_adversos, " +
                    "activa, fecha_creacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE vacunas SET codigo = ?, nombre = ?, nombre_comercial = ?, descripcion = ?, " +
                    "fabricante = ?, dosis_total = ?, via_administracion = ?, sitio_aplicacion = ?, " +
                    "contraindicaciones = ?, efectos_adversos = ?, activa = ? WHERE id = ?";

    private static final String FIND_BY_CODIGO_SQL =
            "SELECT * FROM vacunas WHERE codigo = ?";

    private static final String FIND_BY_CODIGO_ACTIVE_SQL =
            "SELECT * FROM vacunas WHERE codigo = ? AND activa = true";

    private static final String FIND_ACTIVE_SQL =
            "SELECT * FROM vacunas WHERE activa = true ORDER BY codigo";

    private static final String FIND_BY_VIA_ADMINISTRACION_SQL =
            "SELECT * FROM vacunas WHERE via_administracion = ? AND activa = true ORDER BY codigo";

    private static final String FIND_MULTIPLE_DOSES_SQL =
            "SELECT * FROM vacunas WHERE dosis_total > 1 AND activa = true ORDER BY codigo";

    private static final String SEARCH_BY_NAME_SQL =
            "SELECT * FROM vacunas WHERE (nombre LIKE ? OR nombre_comercial LIKE ? OR codigo LIKE ?) " +
                    "AND activa = true ORDER BY codigo";

    private static final String COUNT_ACTIVE_SQL =
            "SELECT COUNT(*) FROM vacunas WHERE activa = true";

    private static final String DEACTIVATE_SQL =
            "UPDATE vacunas SET activa = false WHERE id = ?";

    private static final String ACTIVATE_SQL =
            "UPDATE vacunas SET activa = true WHERE id = ?";

    /**
     * Constructor privado para patrón singleton
     */
    private VacunaDAO() {
        super();
    }

    /**
     * Obtiene la instancia singleton del DAO
     * @return instancia de VacunaDAO
     */
    public static synchronized VacunaDAO getInstance() {
        if (instance == null) {
            instance = new VacunaDAO();
        }
        return instance;
    }

    @Override
    protected String getTableName() {
        return "vacunas";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected Vacuna mapResultSetToEntity(ResultSet rs) throws SQLException {
        Vacuna vacuna = new Vacuna();

        vacuna.setId(getInteger(rs, "id"));
        vacuna.setCodigo(getString(rs, "codigo"));
        vacuna.setNombre(getString(rs, "nombre"));
        vacuna.setNombreComercial(getString(rs, "nombre_comercial"));
        vacuna.setDescripcion(getString(rs, "descripcion"));
        vacuna.setFabricante(getString(rs, "fabricante"));
        vacuna.setDosisTotal(getInteger(rs, "dosis_total"));
        vacuna.setViaAdministracion(getString(rs, "via_administracion"));
        vacuna.setSitioAplicacion(getString(rs, "sitio_aplicacion"));
        vacuna.setContraindicaciones(getString(rs, "contraindicaciones"));
        vacuna.setEfectosAdversos(getString(rs, "efectos_adversos"));
        vacuna.setActiva(getBoolean(rs, "activa"));
        vacuna.setFechaCreacion(toLocalDateTime(rs.getTimestamp("fecha_creacion")));

        return vacuna;
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Vacuna vacuna) throws SQLException {
        ps.setString(1, vacuna.getCodigo());
        ps.setString(2, vacuna.getNombre());
        ps.setString(3, vacuna.getNombreComercial());
        ps.setString(4, vacuna.getDescripcion());
        ps.setString(5, vacuna.getFabricante());
        ps.setInt(6, vacuna.getDosisTotal() != null ? vacuna.getDosisTotal() : 1);
        ps.setString(7, vacuna.getViaAdministracion());
        ps.setString(8, vacuna.getSitioAplicacion());
        ps.setString(9, vacuna.getContraindicaciones());
        ps.setString(10, vacuna.getEfectosAdversos());
        ps.setBoolean(11, vacuna.getActiva() != null ? vacuna.getActiva() : true);
        ps.setTimestamp(12, java.sql.Timestamp.valueOf(
                vacuna.getFechaCreacion() != null ? vacuna.getFechaCreacion() : LocalDateTime.now()));
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, Vacuna vacuna) throws SQLException {
        ps.setString(1, vacuna.getCodigo());
        ps.setString(2, vacuna.getNombre());
        ps.setString(3, vacuna.getNombreComercial());
        ps.setString(4, vacuna.getDescripcion());
        ps.setString(5, vacuna.getFabricante());
        ps.setInt(6, vacuna.getDosisTotal() != null ? vacuna.getDosisTotal() : 1);
        ps.setString(7, vacuna.getViaAdministracion());
        ps.setString(8, vacuna.getSitioAplicacion());
        ps.setString(9, vacuna.getContraindicaciones());
        ps.setString(10, vacuna.getEfectosAdversos());
        ps.setBoolean(11, vacuna.getActiva() != null ? vacuna.getActiva() : true);
        ps.setInt(12, vacuna.getId());
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
    protected void assignGeneratedId(Vacuna vacuna, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            vacuna.setId(generatedKeys.getInt(1));
        }
    }

    // Métodos específicos para vacunas

    /**
     * Busca una vacuna por código
     * @param codigo código de la vacuna (ej: BCG, SRP, etc.)
     * @return Optional con la vacuna encontrada
     */
    public Optional<Vacuna> findByCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return Optional.empty();
        }

        logQuery(FIND_BY_CODIGO_SQL, codigo);
        Vacuna vacuna = executeQuerySingle(FIND_BY_CODIGO_SQL, codigo.trim().toUpperCase());

        if (vacuna != null) {
            logger.debug("Vacuna encontrada por código: {}", codigo);
        } else {
            logger.debug("No se encontró vacuna con código: {}", codigo);
        }

        return Optional.ofNullable(vacuna);
    }

    /**
     * Busca una vacuna activa por código
     * @param codigo código de la vacuna
     * @return Optional con la vacuna encontrada
     */
    public Optional<Vacuna> findByCodigoActive(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return Optional.empty();
        }

        logQuery(FIND_BY_CODIGO_ACTIVE_SQL, codigo);
        Vacuna vacuna = executeQuerySingle(FIND_BY_CODIGO_ACTIVE_SQL, codigo.trim().toUpperCase());

        if (vacuna != null) {
            logger.debug("Vacuna activa encontrada por código: {}", codigo);
        } else {
            logger.debug("No se encontró vacuna activa con código: {}", codigo);
        }

        return Optional.ofNullable(vacuna);
    }

    /**
     * Obtiene todas las vacunas activas
     * @return lista de vacunas activas
     */
    public List<Vacuna> findActiveVaccines() {
        logQuery(FIND_ACTIVE_SQL);
        List<Vacuna> vacunas = executeQuery(FIND_ACTIVE_SQL);

        logger.debug("Encontradas {} vacunas activas", vacunas.size());
        return vacunas;
    }

    /**
     * Busca vacunas por vía de administración
     * @param viaAdministracion vía de administración (Oral, Intramuscular, etc.)
     * @return lista de vacunas con la vía especificada
     */
    public List<Vacuna> findByViaAdministracion(String viaAdministracion) {
        if (viaAdministracion == null || viaAdministracion.trim().isEmpty()) {
            return List.of();
        }

        logQuery(FIND_BY_VIA_ADMINISTRACION_SQL, viaAdministracion);
        List<Vacuna> vacunas = executeQuery(FIND_BY_VIA_ADMINISTRACION_SQL, viaAdministracion.trim());

        logger.debug("Encontradas {} vacunas con vía de administración: {}", vacunas.size(), viaAdministracion);
        return vacunas;
    }

    /**
     * Obtiene vacunas que requieren múltiples dosis
     * @return lista de vacunas con más de una dosis
     */
    public List<Vacuna> findMultipleDosesVaccines() {
        logQuery(FIND_MULTIPLE_DOSES_SQL);
        List<Vacuna> vacunas = executeQuery(FIND_MULTIPLE_DOSES_SQL);

        logger.debug("Encontradas {} vacunas que requieren múltiples dosis", vacunas.size());
        return vacunas;
    }

    /**
     * Busca vacunas por nombre, nombre comercial o código (búsqueda parcial)
     * @param searchTerm término de búsqueda
     * @return lista de vacunas que coinciden con la búsqueda
     */
    public List<Vacuna> searchByName(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findActiveVaccines();
        }

        String searchPattern = "%" + searchTerm.trim().toLowerCase() + "%";
        logQuery(SEARCH_BY_NAME_SQL, searchPattern, searchPattern, searchPattern);

        List<Vacuna> vacunas = executeQuery(SEARCH_BY_NAME_SQL, searchPattern, searchPattern, searchPattern);

        logger.debug("Encontradas {} vacunas que coinciden con: {}", vacunas.size(), searchTerm);
        return vacunas;
    }

    /**
     * Cuenta el número de vacunas activas
     * @return número de vacunas activas
     */
    public long countActiveVaccines() {
        logQuery(COUNT_ACTIVE_SQL);
        Object result = executeScalar(COUNT_ACTIVE_SQL);

        long count = result instanceof Number ? ((Number) result).longValue() : 0;
        logger.debug("Conteo de vacunas activas: {}", count);

        return count;
    }

    /**
     * Desactiva una vacuna (soft delete)
     * @param vacunaId ID de la vacuna a desactivar
     * @return true si se desactivó correctamente
     */
    public boolean deactivateVaccine(Integer vacunaId) {
        if (vacunaId == null) {
            return false;
        }

        try {
            int affectedRows = executeUpdate(DEACTIVATE_SQL, vacunaId);
            boolean deactivated = affectedRows > 0;

            if (deactivated) {
                logger.info("Vacuna desactivada ID: {}", vacunaId);
            } else {
                logger.warn("No se pudo desactivar la vacuna ID: {}", vacunaId);
            }

            return deactivated;

        } catch (Exception e) {
            logger.error("Error al desactivar vacuna ID: {}", vacunaId, e);
            return false;
        }
    }

    /**
     * Activa una vacuna
     * @param vacunaId ID de la vacuna a activar
     * @return true si se activó correctamente
     */
    public boolean activateVaccine(Integer vacunaId) {
        if (vacunaId == null) {
            return false;
        }

        try {
            int affectedRows = executeUpdate(ACTIVATE_SQL, vacunaId);
            boolean activated = affectedRows > 0;

            if (activated) {
                logger.info("Vacuna activada ID: {}", vacunaId);
            } else {
                logger.warn("No se pudo activar la vacuna ID: {}", vacunaId);
            }

            return activated;

        } catch (Exception e) {
            logger.error("Error al activar vacuna ID: {}", vacunaId, e);
            return false;
        }
    }

    /**
     * Verifica si un código de vacuna ya existe
     * @param codigo código a verificar
     * @return true si ya existe
     */
    public boolean existsByCodigo(String codigo) {
        return findByCodigo(codigo).isPresent();
    }

    /**
     * Verifica si un código de vacuna ya existe excluyendo una vacuna específica
     * @param codigo código a verificar
     * @param excludeVacunaId ID de la vacuna a excluir de la verificación
     * @return true si ya existe en otra vacuna
     */
    public boolean existsByCodigoExcluding(String codigo, Integer excludeVacunaId) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM vacunas WHERE codigo = ? AND id != ?";
        logQuery(sql, codigo, excludeVacunaId);

        Object result = executeScalar(sql, codigo.trim().toUpperCase(), excludeVacunaId);
        return result instanceof Number && ((Number) result).longValue() > 0;
    }

    /**
     * Obtiene vacunas orales (vía oral)
     * @return lista de vacunas orales
     */
    public List<Vacuna> findOralVaccines() {
        return findByViaAdministracion("Oral");
    }

    /**
     * Obtiene vacunas inyectables (intramusculares, subcutáneas, intradérmicas)
     * @return lista de vacunas inyectables
     */
    public List<Vacuna> findInjectableVaccines() {
        String sql = "SELECT * FROM vacunas WHERE via_administracion IN ('Intramuscular', 'Subcutánea', 'Intradérmica') " +
                "AND activa = true ORDER BY codigo";

        logQuery(sql);
        List<Vacuna> vacunas = executeQuery(sql);

        logger.debug("Encontradas {} vacunas inyectables", vacunas.size());
        return vacunas;
    }

    /**
     * Obtiene vacunas del esquema básico ecuatoriano (las más importantes)
     * @return lista de vacunas del esquema básico
     */
    public List<Vacuna> findBasicScheduleVaccines() {
        String sql = "SELECT * FROM vacunas WHERE codigo IN ('BCG', 'HB0', 'ROTA', 'IPV', 'NEUMO', 'PENTA', 'bOPV', 'INFLUENZA', 'SRP', 'FA', 'VARICELA') " +
                "AND activa = true ORDER BY " +
                "CASE codigo " +
                "WHEN 'BCG' THEN 1 " +
                "WHEN 'HB0' THEN 2 " +
                "WHEN 'ROTA' THEN 3 " +
                "WHEN 'IPV' THEN 4 " +
                "WHEN 'NEUMO' THEN 5 " +
                "WHEN 'PENTA' THEN 6 " +
                "WHEN 'bOPV' THEN 7 " +
                "WHEN 'INFLUENZA' THEN 8 " +
                "WHEN 'SRP' THEN 9 " +
                "WHEN 'FA' THEN 10 " +
                "WHEN 'VARICELA' THEN 11 " +
                "ELSE 99 END";

        logQuery(sql);
        List<Vacuna> vacunas = executeQuery(sql);

        logger.debug("Encontradas {} vacunas del esquema básico ecuatoriano", vacunas.size());
        return vacunas;
    }

    /**
     * Obtiene estadísticas de vacunas
     * @return String con estadísticas formateadas
     */
    public String getVaccineStatistics() {
        StringBuilder stats = new StringBuilder();

        // Total de vacunas
        long totalVaccines = count();
        long activeVaccines = countActiveVaccines();
        long inactiveVaccines = totalVaccines - activeVaccines;

        stats.append("ESTADÍSTICAS DE VACUNAS\n");
        stats.append("=======================\n");
        stats.append("Total vacunas: ").append(totalVaccines).append("\n");
        stats.append("Vacunas activas: ").append(activeVaccines).append("\n");
        stats.append("Vacunas inactivas: ").append(inactiveVaccines).append("\n");

        // Vacunas por vía de administración
        List<Vacuna> orales = findOralVaccines();
        List<Vacuna> inyectables = findInjectableVaccines();
        List<Vacuna> multiplesDosis = findMultipleDosesVaccines();

        stats.append("\nPOR VÍA DE ADMINISTRACIÓN\n");
        stats.append("=========================\n");
        stats.append("Vacunas orales: ").append(orales.size()).append("\n");
        stats.append("Vacunas inyectables: ").append(inyectables.size()).append("\n");
        stats.append("Requieren múltiples dosis: ").append(multiplesDosis.size()).append("\n");

        return stats.toString();
    }

    /**
     * Inicializa las vacunas básicas del esquema ecuatoriano si no existen
     * @return número de vacunas creadas
     */
    public int initializeBasicVaccines() {
        int created = 0;

        try {
            // Lista de vacunas básicas del esquema ecuatoriano
            Vacuna[] basicVaccines = {
                    Vacuna.crearBCG(),
                    Vacuna.crearHepatitisB(),
                    Vacuna.crearRotavirus(),
                    Vacuna.crearPolioIPV(),
                    Vacuna.crearNeumococo(),
                    Vacuna.crearPentavalente(),
                    Vacuna.crearSRP(),
                    Vacuna.crearVaricela()
            };

            for (Vacuna vacuna : basicVaccines) {
                if (!existsByCodigo(vacuna.getCodigo())) {
                    insert(vacuna);
                    created++;
                    logger.info("Vacuna básica creada: {}", vacuna.getCodigo());
                }
            }

            logger.info("Inicialización de vacunas completada. Creadas: {}", created);

        } catch (Exception e) {
            logger.error("Error al inicializar vacunas básicas", e);
        }

        return created;
    }
}
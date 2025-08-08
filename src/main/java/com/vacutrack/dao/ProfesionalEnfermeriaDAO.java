package com.vacutrack.dao;

import com.vacutrack.model.ProfesionalEnfermeria;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO para la entidad ProfesionalEnfermeria
 * Maneja operaciones de base de datos para profesionales de enfermería
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class ProfesionalEnfermeriaDAO extends BaseDAO<ProfesionalEnfermeria, Integer> {

    // Instancia singleton
    private static ProfesionalEnfermeriaDAO instance;

    // SQL Queries
    private static final String INSERT_SQL =
            "INSERT INTO profesionales_enfermeria (usuario_id, nombres, apellidos, " +
            "numero_colegio, centro_salud_id, especialidad, fecha_creacion) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE profesionales_enfermeria SET nombres = ?, apellidos = ?, " +
            "numero_colegio = ?, centro_salud_id = ?, especialidad = ? " +
            "WHERE id = ?";

    private static final String FIND_BY_USUARIO_ID_SQL =
            "SELECT pe.*, u.cedula, u.email, u.activo as usuario_activo, " +
                    "cs.nombre as centro_nombre, cs.direccion as centro_direccion " +
                    "FROM profesionales_enfermeria pe " +
                    "LEFT JOIN usuarios u ON pe.usuario_id = u.id " +
                    "LEFT JOIN centros_salud cs ON pe.centro_salud_id = cs.id " +
                    "WHERE pe.usuario_id = ?";

    private static final String FIND_BY_NUMERO_COLEGIO_SQL =
            "SELECT * FROM profesionales_enfermeria WHERE numero_colegio = ?";

    private static final String FIND_BY_CENTRO_SALUD_SQL =
            "SELECT * FROM profesionales_enfermeria WHERE centro_salud_id = ? " +
            "ORDER BY apellidos, nombres";

    private static final String FIND_ACTIVE_SQL =
            "SELECT pe.*, u.cedula, u.email, u.activo as usuario_activo, " +
                    "cs.nombre as centro_nombre, cs.direccion as centro_direccion " +
                    "FROM profesionales_enfermeria pe " +
                    "LEFT JOIN usuarios u ON pe.usuario_id = u.id " +
                    "LEFT JOIN centros_salud cs ON pe.centro_salud_id = cs.id " +
                    "WHERE u.activo = true " +
                    "ORDER BY pe.apellidos, pe.nombres";

    private static final String SEARCH_BY_NAME_SQL =
            "SELECT * FROM profesionales_enfermeria WHERE (nombres LIKE ? OR apellidos LIKE ?) " +
            "ORDER BY apellidos, nombres";

    private static final String COUNT_BY_CENTRO_SALUD_SQL =
            "SELECT COUNT(*) FROM profesionales_enfermeria WHERE centro_salud_id = ?";

    /**
     * Constructor privado para patrón singleton
     */
    private ProfesionalEnfermeriaDAO() {
        super();
    }

    /**
     * Obtiene la instancia singleton del DAO
     * @return instancia de ProfesionalEnfermeriaDAO
     */
    public static synchronized ProfesionalEnfermeriaDAO getInstance() {
        if (instance == null) {
            instance = new ProfesionalEnfermeriaDAO();
        }
        return instance;
    }

    @Override
    protected String getTableName() {
        return "profesionales_enfermeria";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected ProfesionalEnfermeria mapResultSetToEntity(ResultSet rs) throws SQLException {
        ProfesionalEnfermeria profesional = new ProfesionalEnfermeria();

        profesional.setId(getInteger(rs, "id"));
        profesional.setUsuarioId(getInteger(rs, "usuario_id"));
        profesional.setNombres(getString(rs, "nombres"));
        profesional.setApellidos(getString(rs, "apellidos"));
        profesional.setNumeroColegio(getString(rs, "numero_colegio"));
        profesional.setCentroSaludId(getInteger(rs, "centro_salud_id"));
        profesional.setEspecialidad(getString(rs, "especialidad"));
        profesional.setFechaCreacion(toLocalDateTime(rs.getTimestamp("fecha_creacion")));

        return profesional;
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, ProfesionalEnfermeria profesional) throws SQLException {
        ps.setInt(1, profesional.getUsuarioId());
        ps.setString(2, profesional.getNombres());
        ps.setString(3, profesional.getApellidos());
        ps.setString(4, profesional.getNumeroColegio());

        // ✅ CORREGIR: Manejar centroSaludId que puede ser null
        if (profesional.getCentroSaludId() != null) {
            ps.setInt(5, profesional.getCentroSaludId());
        } else {
            ps.setNull(5, Types.INTEGER);
        }

        ps.setString(6, profesional.getEspecialidad());
        ps.setTimestamp(7, java.sql.Timestamp.valueOf(
                profesional.getFechaCreacion() != null ? profesional.getFechaCreacion() : LocalDateTime.now()));
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, ProfesionalEnfermeria profesional) throws SQLException {
        ps.setString(1, profesional.getNombres());
        ps.setString(2, profesional.getApellidos());
        ps.setString(3, profesional.getNumeroColegio());

        // ✅ CORREGIR: Manejar centroSaludId que puede ser null
        if (profesional.getCentroSaludId() != null) {
            ps.setInt(4, profesional.getCentroSaludId());
        } else {
            ps.setNull(4, Types.INTEGER);
        }

        ps.setString(5, profesional.getEspecialidad());
        ps.setInt(6, profesional.getId());
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
    protected void assignGeneratedId(ProfesionalEnfermeria profesional, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            profesional.setId(generatedKeys.getInt(1));
        }
    }

    // Métodos específicos para profesionales de enfermería

    /**
     * Busca un profesional por ID de usuario
     * @param usuarioId ID del usuario
     * @return Optional con el profesional encontrado
     */
    public Optional<ProfesionalEnfermeria> findByUsuarioId(Integer usuarioId) {
        if (usuarioId == null) {
            return Optional.empty();
        }

        logQuery(FIND_BY_USUARIO_ID_SQL, usuarioId);
        ProfesionalEnfermeria profesional = executeQuerySingle(FIND_BY_USUARIO_ID_SQL, usuarioId);
        
        Optional<ProfesionalEnfermeria> result = Optional.ofNullable(profesional);
        logger.debug("Profesional encontrado por usuario {}: {}", usuarioId, result.isPresent());
        
        return result;
    }

    /**
     * Busca un profesional por número de colegio
     * @param numeroColegio número de colegio profesional
     * @return Optional con el profesional encontrado
     */
    public Optional<ProfesionalEnfermeria> findByNumeroColegio(String numeroColegio) {
        if (numeroColegio == null || numeroColegio.trim().isEmpty()) {
            return Optional.empty();
        }

        logQuery(FIND_BY_NUMERO_COLEGIO_SQL, numeroColegio);
        ProfesionalEnfermeria profesional = executeQuerySingle(FIND_BY_NUMERO_COLEGIO_SQL, numeroColegio.trim());
        
        Optional<ProfesionalEnfermeria> result = Optional.ofNullable(profesional);
        logger.debug("Profesional encontrado por número de colegio {}: {}", numeroColegio, result.isPresent());
        
        return result;
    }

    /**
     * Busca profesionales por centro de salud
     * @param centroSaludId ID del centro de salud
     * @return lista de profesionales del centro de salud
     */
    public List<ProfesionalEnfermeria> findByCentroSalud(Integer centroSaludId) {
        if (centroSaludId == null) {
            return List.of();
        }

        logQuery(FIND_BY_CENTRO_SALUD_SQL, centroSaludId);
        List<ProfesionalEnfermeria> profesionales = executeQuery(FIND_BY_CENTRO_SALUD_SQL, centroSaludId);
        
        logger.debug("Encontrados {} profesionales en centro de salud ID {}", profesionales.size(), centroSaludId);
        return profesionales;
    }

    /**
     * Obtiene todos los profesionales activos
     * @return lista de profesionales activos
     */
    public List<ProfesionalEnfermeria> findActive() {
        logQuery(FIND_ACTIVE_SQL);
        List<ProfesionalEnfermeria> profesionales = executeQuery(FIND_ACTIVE_SQL);
        
        logger.debug("Encontrados {} profesionales activos", profesionales.size());
        return profesionales;
    }

    /**
     * Busca profesionales por nombre (búsqueda parcial)
     * @param searchTerm término de búsqueda
     * @return lista de profesionales que coinciden con la búsqueda
     */
    public List<ProfesionalEnfermeria> searchByName(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        String searchPattern = "%" + searchTerm.trim() + "%";
        logQuery(SEARCH_BY_NAME_SQL, searchPattern, searchPattern);
        List<ProfesionalEnfermeria> profesionales = executeQuery(SEARCH_BY_NAME_SQL, searchPattern, searchPattern);
        
        logger.debug("Encontrados {} profesionales con término '{}'", profesionales.size(), searchTerm);
        return profesionales;
    }

    /**
     * Cuenta profesionales por centro de salud
     * @param centroSaludId ID del centro de salud
     * @return número de profesionales en el centro de salud
     */
    public long countByCentroSalud(Integer centroSaludId) {
        if (centroSaludId == null) {
            return 0;
        }

        logQuery(COUNT_BY_CENTRO_SALUD_SQL, centroSaludId);
        Object result = executeScalar(COUNT_BY_CENTRO_SALUD_SQL, centroSaludId);

        long count = result instanceof Number ? ((Number) result).longValue() : 0;
        logger.debug("Conteo de profesionales en centro de salud ID {}: {}", centroSaludId, count);

        return count;
    }

    /**
     * Verifica si existe un profesional con el número de colegio dado
     * @param numeroColegio número de colegio a verificar
     * @return true si existe el profesional
     */
    public boolean existsByNumeroColegio(String numeroColegio) {
        return findByNumeroColegio(numeroColegio).isPresent();
    }

    /**
     * Verifica si existe un profesional para el usuario dado
     * @param usuarioId ID del usuario
     * @return true si existe el profesional
     */
    public boolean existsByUsuarioId(Integer usuarioId) {
        return findByUsuarioId(usuarioId).isPresent();
    }

    /**
     * Obtiene lista de centros de salud únicos donde hay profesionales
     * @return lista de IDs de centros de salud
     */
    public List<Integer> getCentrosSaludConProfesionales() {
        String sql = "SELECT DISTINCT centro_salud_id FROM profesionales_enfermeria " +
                "WHERE centro_salud_id IS NOT NULL " +
                "ORDER BY centro_salud_id";

        logQuery(sql);

        // ✅ CORREGIR: Usar databaseConfig en lugar de getConnection()
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Integer> centros = new java.util.ArrayList<>();
            while (rs.next()) {
                Integer centroId = rs.getInt("centro_salud_id");
                if (!rs.wasNull() && centroId > 0) {  // ✅ MEJORAR: Usar wasNull()
                    centros.add(centroId);
                }
            }

            logger.debug("Encontrados {} centros de salud con profesionales", centros.size());
            return centros;

        } catch (SQLException e) {
            logger.error("Error al obtener lista de centros de salud", e);
            return List.of();
        }
    }

    /**
     * Obtiene estadísticas de profesionales
     * @return String con estadísticas formateadas
     */
    public String getProfesionalStatistics() {
        StringBuilder stats = new StringBuilder();

        try {
            long totalProfesionales = count();
            long activeProfesionales = findActive().size();

            stats.append("ESTADÍSTICAS DE PROFESIONALES\n");
            stats.append("=============================\n");
            stats.append("Total profesionales: ").append(totalProfesionales).append("\n");
            stats.append("Profesionales registrados: ").append(activeProfesionales).append("\n");

            // Estadísticas por centro de salud
            List<Integer> centros = getCentrosSaludConProfesionales();
            if (!centros.isEmpty()) {
                stats.append("\nPOR CENTRO DE SALUD\n");
                stats.append("===================\n");
                for (Integer centroId : centros) {
                    long count = countByCentroSalud(centroId);
                    stats.append("Centro ID ").append(centroId).append(": ").append(count).append("\n");
                }
            }

        } catch (Exception e) {
            logger.error("Error al obtener estadísticas de profesionales", e);
            stats.append("Error al generar estadísticas");
        }

        return stats.toString();
    }
    // ✅ AGREGAR después del constructor:
    @Override
    protected boolean isNew(ProfesionalEnfermeria entity) {
        return entity.getId() == null;
    }

    // ✅ AGREGAR validación antes de guardar:
    public boolean validateForSave(ProfesionalEnfermeria profesional) {
        if (profesional == null) {
            return false;
        }

        if (!profesional.esValido()) {
            logger.warn("Datos del profesional no válidos: {}", profesional.obtenerMensajesValidacion());
            return false;
        }

        // Validar unicidad de número de colegio si existe
        if (profesional.getNumeroColegio() != null) {
            if (profesional.getId() == null) {
                // Nuevo registro
                if (existsByNumeroColegio(profesional.getNumeroColegio())) {
                    logger.warn("El número de colegio {} ya está registrado", profesional.getNumeroColegio());
                    return false;
                }
            } else {
                // Actualización - verificar que no existe en otro profesional
                String sql = "SELECT COUNT(*) FROM profesionales_enfermeria WHERE numero_colegio = ? AND id != ?";
                Object result = executeScalar(sql, profesional.getNumeroColegio(), profesional.getId());
                if (result instanceof Number && ((Number) result).longValue() > 0) {
                    logger.warn("El número de colegio {} ya está registrado en otro profesional", profesional.getNumeroColegio());
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean save(ProfesionalEnfermeria profesional) {
        if (!validateForSave(profesional)) {
            return false;
        }
        return super.save(profesional);
    }

    @Override
    public boolean update(ProfesionalEnfermeria profesional) {
        if (!validateForSave(profesional)) {
            return false;
        }
        return super.update(profesional);
    }

    // ✅ AGREGAR para consistencia con otros DAOs:
    public long countActivos() {
        String sql = "SELECT COUNT(*) FROM profesionales_enfermeria pe " +
                "INNER JOIN usuarios u ON pe.usuario_id = u.id " +
                "WHERE u.activo = true";

        logQuery(sql);
        Object result = executeScalar(sql);

        long count = result instanceof Number ? ((Number) result).longValue() : 0;
        logger.debug("Total profesionales activos: {}", count);

        return count;
    }
}
package com.vacutrack.dao;

import com.vacutrack.model.PadreFamilia;
import com.vacutrack.model.Usuario;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO para la entidad PadreFamilia
 * Maneja operaciones de base de datos relacionadas con padres de familia
 * Incluye métodos específicos para búsquedas por usuario, teléfono, etc.
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class PadreFamiliaDAO extends BaseDAO<PadreFamilia, Integer> {

    // Instancia singleton
    private static PadreFamiliaDAO instance;

    // SQL Queries
    private static final String INSERT_SQL =
            "INSERT INTO padres_familia (usuario_id, nombres, apellidos, telefono, direccion, fecha_creacion) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE padres_familia SET usuario_id = ?, nombres = ?, apellidos = ?, " +
                    "telefono = ?, direccion = ? WHERE id = ?";

    private static final String FIND_BY_USUARIO_SQL =
            "SELECT pf.*, u.cedula, u.email, u.activo as usuario_activo " +
                    "FROM padres_familia pf " +
                    "LEFT JOIN usuarios u ON pf.usuario_id = u.id " +
                    "WHERE pf.usuario_id = ?";

    private static final String FIND_ACTIVE_SQL =
            "SELECT pf.*, u.cedula, u.email, u.activo as usuario_activo " +
                    "FROM padres_familia pf " +
                    "LEFT JOIN usuarios u ON pf.usuario_id = u.id " +
                    "WHERE u.activo = true " +
                    "ORDER BY pf.nombres, pf.apellidos";

    private static final String COUNT_ACTIVE_SQL =
            "SELECT COUNT(*) FROM padres_familia pf " +
                    "INNER JOIN usuarios u ON pf.usuario_id = u.id " +
                    "WHERE u.activo = true";

    private static final String SEARCH_BY_NAME_SQL =
            "SELECT pf.*, u.cedula, u.email, u.activo as usuario_activo " +
                    "FROM padres_familia pf " +
                    "LEFT JOIN usuarios u ON pf.usuario_id = u.id " +
                    "WHERE u.activo = true " +
                    "AND (pf.nombres LIKE ? OR pf.apellidos LIKE ? " +
                    "OR CONCAT(pf.nombres, ' ', pf.apellidos) LIKE ?) " +
                    "ORDER BY pf.nombres, pf.apellidos";

    private static final String FIND_BY_TELEFONO_SQL =
            "SELECT pf.*, u.cedula, u.email, u.activo as usuario_activo " +
                    "FROM padres_familia pf " +
                    "LEFT JOIN usuarios u ON pf.usuario_id = u.id " +
                    "WHERE pf.telefono = ? AND u.activo = true";

    private static final String FIND_BY_CEDULA_SQL =
            "SELECT pf.*, u.cedula, u.email, u.activo as usuario_activo " +
                    "FROM padres_familia pf " +
                    "INNER JOIN usuarios u ON pf.usuario_id = u.id " +
                    "WHERE u.cedula = ? AND u.activo = true";

    private static final String FIND_RECENT_REGISTRATIONS_SQL =
            "SELECT pf.*, u.cedula, u.email, u.activo as usuario_activo " +
                    "FROM padres_familia pf " +
                    "LEFT JOIN usuarios u ON pf.usuario_id = u.id " +
                    "WHERE u.activo = true " +
                    "AND pf.fecha_creacion >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                    "ORDER BY pf.fecha_creacion DESC";

    private static final String FIND_WITH_CONTACT_INFO_SQL =
            "SELECT pf.*, u.cedula, u.email, u.activo as usuario_activo " +
                    "FROM padres_familia pf " +
                    "LEFT JOIN usuarios u ON pf.usuario_id = u.id " +
                    "WHERE u.activo = true " +
                    "AND (pf.telefono IS NOT NULL OR pf.direccion IS NOT NULL) " +
                    "ORDER BY pf.nombres, pf.apellidos";

    /**
     * Constructor privado para patrón singleton
     */
    private PadreFamiliaDAO() {
        super();
    }

    /**
     * Obtiene la instancia singleton del DAO
     * @return instancia de PadreFamiliaDAO
     */
    public static synchronized PadreFamiliaDAO getInstance() {
        if (instance == null) {
            instance = new PadreFamiliaDAO();
        }
        return instance;
    }

    @Override
    protected String getTableName() {
        return "padres_familia";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected PadreFamilia mapResultSetToEntity(ResultSet rs) throws SQLException {
        PadreFamilia padre = new PadreFamilia();

        // Datos básicos del padre de familia
        padre.setId(getInteger(rs, "id"));
        padre.setUsuarioId(getInteger(rs, "usuario_id"));
        padre.setNombres(getString(rs, "nombres"));
        padre.setApellidos(getString(rs, "apellidos"));
        padre.setTelefono(getString(rs, "telefono"));
        padre.setDireccion(getString(rs, "direccion"));
        padre.setFechaCreacion(toLocalDateTime(rs.getTimestamp("fecha_creacion")));

        // Mapear información básica del usuario si está disponible
        try {
            String cedula = getString(rs, "cedula");
            String email = getString(rs, "email");
            Boolean usuarioActivo = getBoolean(rs, "usuario_activo");

            if (cedula != null || email != null) {
                Usuario usuario = new Usuario();
                usuario.setId(padre.getUsuarioId());
                usuario.setCedula(cedula);
                usuario.setEmail(email);
                usuario.setActivo(usuarioActivo != null ? usuarioActivo : true);
                padre.setUsuario(usuario);
            }
        } catch (SQLException e) {
            // Información del usuario no disponible en esta consulta, ignorar
            logger.debug("Información del usuario no disponible en el ResultSet");
        }

        return padre;
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, PadreFamilia padre) throws SQLException {
        ps.setInt(1, padre.getUsuarioId());
        ps.setString(2, padre.getNombres());
        ps.setString(3, padre.getApellidos());
        ps.setString(4, padre.getTelefono());
        ps.setString(5, padre.getDireccion());
        ps.setTimestamp(6, java.sql.Timestamp.valueOf(
                padre.getFechaCreacion() != null ? padre.getFechaCreacion() : LocalDateTime.now()));
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, PadreFamilia padre) throws SQLException {
        ps.setInt(1, padre.getUsuarioId());
        ps.setString(2, padre.getNombres());
        ps.setString(3, padre.getApellidos());
        ps.setString(4, padre.getTelefono());
        ps.setString(5, padre.getDireccion());
        ps.setInt(6, padre.getId());
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
    protected void assignGeneratedId(PadreFamilia padre, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            padre.setId(generatedKeys.getInt(1));
        }
    }
    /**
     * Valida si los datos del padre son consistentes antes de guardar
     * @param padre padre a validar
     * @return true si es válido para guardar
     */
    public boolean validateForSave(PadreFamilia padre) {
        if (padre == null) {
            return false;
        }

        // Validar datos básicos
        if (!padre.esValido()) {
            logger.warn("Datos del padre no válidos: {}", padre.obtenerMensajesValidacion());
            return false;
        }

        // Validar unicidad de teléfono si existe
        if (padre.getTelefono() != null && !padre.getTelefono().trim().isEmpty()) {
            if (padre.getId() == null) {
                // Nuevo registro
                if (existsByTelefono(padre.getTelefono())) {
                    logger.warn("El teléfono {} ya está registrado", padre.getTelefono());
                    return false;
                }
            } else {
                // Actualización
                if (existsByTelefonoExcluding(padre.getTelefono(), padre.getId())) {
                    logger.warn("El teléfono {} ya está registrado en otro padre", padre.getTelefono());
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean save(PadreFamilia padre) {
        if (!validateForSave(padre)) {
            return false;
        }
        return super.save(padre);
    }

    @Override
    public boolean update(PadreFamilia padre) {
        if (!validateForSave(padre)) {
            return false;
        }
        return super.update(padre);
    }
    // ========== MÉTODOS ESPECÍFICOS PARA REPORTESERVICE ==========

    /**
     * Cuenta padres activos - REQUERIDO por ReporteService
     * @return número total de padres activos
     */

    public long countActivos() {  // Era: public int countActivos()
        logQuery(COUNT_ACTIVE_SQL);
        Object result = executeScalar(COUNT_ACTIVE_SQL);

        long count = result instanceof Number ? ((Number) result).longValue() : 0;  // Era: intValue()
        logger.debug("Total padres activos: {}", count);

        return count;
    }

    /**
     * Obtiene todos los padres activos - REQUERIDO por ReporteService
     * @return lista de padres activos
     */
    public List<PadreFamilia> findActive() {
        logQuery(FIND_ACTIVE_SQL);
        List<PadreFamilia> padres = executeQuery(FIND_ACTIVE_SQL);

        logger.debug("Encontrados {} padres activos", padres.size());
        return padres;
    }

    // ========== MÉTODOS ESPECÍFICOS ADICIONALES ==========

    /**
     * Busca padre de familia por usuario ID
     * @param usuarioId ID del usuario
     * @return Optional con el padre encontrado
     */
    public Optional<PadreFamilia> findByUsuario(Integer usuarioId) {
        if (usuarioId == null) {
            return Optional.empty();
        }

        logQuery(FIND_BY_USUARIO_SQL, usuarioId);
        PadreFamilia padre = executeQuerySingle(FIND_BY_USUARIO_SQL, usuarioId);

        if (padre != null) {
            logger.debug("Padre encontrado para usuario ID: {}", usuarioId);
        } else {
            logger.debug("No se encontró padre para usuario ID: {}", usuarioId);
        }

        return Optional.ofNullable(padre);
    }

    /**
     * Busca padre de familia por cédula
     * @param cedula cédula del usuario asociado
     * @return Optional con el padre encontrado
     */
    public Optional<PadreFamilia> findByCedula(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            return Optional.empty();
        }

        logQuery(FIND_BY_CEDULA_SQL, cedula);
        PadreFamilia padre = executeQuerySingle(FIND_BY_CEDULA_SQL, cedula.trim());

        if (padre != null) {
            logger.debug("Padre encontrado por cédula: {}", cedula);
        } else {
            logger.debug("No se encontró padre con cédula: {}", cedula);
        }

        return Optional.ofNullable(padre);
    }

    /**
     * Busca padre de familia por teléfono
     * @param telefono teléfono del padre
     * @return Optional con el padre encontrado
     */
    public Optional<PadreFamilia> findByTelefono(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) {
            return Optional.empty();
        }

        logQuery(FIND_BY_TELEFONO_SQL, telefono);
        PadreFamilia padre = executeQuerySingle(FIND_BY_TELEFONO_SQL, telefono.trim());

        if (padre != null) {
            logger.debug("Padre encontrado por teléfono: {}", telefono);
        } else {
            logger.debug("No se encontró padre con teléfono: {}", telefono);
        }

        return Optional.ofNullable(padre);
    }

    /**
     * Busca padres por nombre (búsqueda parcial)
     * @param termino término de búsqueda
     * @return lista de padres que coinciden con el término
     */
    public List<PadreFamilia> searchByName(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            return List.of();
        }

        String searchTerm = "%" + termino.trim() + "%";
        logQuery(SEARCH_BY_NAME_SQL, searchTerm, searchTerm, searchTerm);
        List<PadreFamilia> padres = executeQuery(SEARCH_BY_NAME_SQL, searchTerm, searchTerm, searchTerm);

        logger.debug("Encontrados {} padres con término de búsqueda: {}", padres.size(), termino);
        return padres;
    }

    /**
     * Busca padres registrados recientemente
     * @param days número de días hacia atrás
     * @return lista de padres registrados en los últimos días
     */
    public List<PadreFamilia> findRecentRegistrations(int days) {
        logQuery(FIND_RECENT_REGISTRATIONS_SQL, days);
        List<PadreFamilia> padres = executeQuery(FIND_RECENT_REGISTRATIONS_SQL, days);

        logger.debug("Encontrados {} padres registrados en los últimos {} días", padres.size(), days);
        return padres;
    }

    /**
     * Busca padres que tienen información de contacto
     * @return lista de padres con teléfono o dirección
     */
    public List<PadreFamilia> findWithContactInfo() {
        logQuery(FIND_WITH_CONTACT_INFO_SQL);
        List<PadreFamilia> padres = executeQuery(FIND_WITH_CONTACT_INFO_SQL);

        logger.debug("Encontrados {} padres con información de contacto", padres.size());
        return padres;
    }

    /**
     * Verifica si una cédula ya está registrada
     * @param cedula cédula a verificar
     * @return true si ya existe
     */
    public boolean existsByCedula(String cedula) {
        return findByCedula(cedula).isPresent();
    }

    /**
     * Verifica si un teléfono ya está registrado
     * @param telefono teléfono a verificar
     * @return true si ya existe
     */
    public boolean existsByTelefono(String telefono) {
        return findByTelefono(telefono).isPresent();
    }

    /**
     * Verifica si un teléfono ya está registrado excluyendo un padre específico
     * @param telefono teléfono a verificar
     * @param excludePadreId ID del padre a excluir
     * @return true si ya existe en otro padre
     */
    public boolean existsByTelefonoExcluding(String telefono, Integer excludePadreId) {
        if (telefono == null || telefono.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM padres_familia pf " +
                "INNER JOIN usuarios u ON pf.usuario_id = u.id " +
                "WHERE pf.telefono = ? AND pf.id != ? AND u.activo = true";

        logQuery(sql, telefono, excludePadreId);
        Object result = executeScalar(sql, telefono.trim(), excludePadreId);
        return result instanceof Number && ((Number) result).longValue() > 0;
    }

    /**
     * Actualiza información de contacto de un padre
     * @param padreId ID del padre
     * @param telefono nuevo teléfono
     * @param direccion nueva dirección
     * @return true si se actualizó correctamente
     */
    // Modificar updateContactInfo para validar parámetros:
    public boolean updateContactInfo(Integer padreId, String telefono, String direccion) {
        if (padreId == null) {
            return false;
        }

        // Validar que al menos uno no sea null/vacío
        if ((telefono == null || telefono.trim().isEmpty()) &&
                (direccion == null || direccion.trim().isEmpty())) {
            logger.warn("Tanto teléfono como dirección están vacíos");
            return false;
        }

        String sql = "UPDATE padres_familia SET telefono = ?, direccion = ? WHERE id = ?";

        try {
            int affectedRows = executeUpdate(sql, telefono, direccion, padreId);
            boolean updated = affectedRows > 0;

            if (updated) {
                logger.info("Información de contacto actualizada para padre ID: {}", padreId);
            } else {
                logger.warn("No se pudo actualizar información de contacto para padre ID: {}", padreId);
            }

            return updated;

        } catch (Exception e) {
            logger.error("Error al actualizar información de contacto para padre ID: {}", padreId, e);
            return false;
        }
    }

    @Override
    protected boolean isNew(PadreFamilia entity) {
        return entity.getId() == null;
    }

    /**
     * Obtiene estadísticas de padres registrados por mes
     * @return String con estadísticas por mes
     */
    // Reemplazar getRegistrationStatistics() con query más portable:
    public String getRegistrationStatistics() {
        String sql = """
        SELECT 
            YEAR(fecha_creacion) as anio,
            MONTH(fecha_creacion) as mes,
            COUNT(*) as total
        FROM padres_familia pf
        INNER JOIN usuarios u ON pf.usuario_id = u.id
        WHERE u.activo = true
        AND fecha_creacion >= DATE_SUB(NOW(), INTERVAL 12 MONTH)
        GROUP BY YEAR(fecha_creacion), MONTH(fecha_creacion)
        ORDER BY anio DESC, mes DESC
        """;

        StringBuilder stats = new StringBuilder();
        stats.append("Registros de Padres por Mes (Últimos 12 meses):\n");
        stats.append("===============================================\n");

        try (var conn = databaseConfig.getConnection();
             var ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {

            while (rs.next()) {
                int anio = rs.getInt("anio");
                int mes = rs.getInt("mes");
                int total = rs.getInt("total");
                String nombreMes = obtenerNombreMes(mes);
                stats.append(String.format("- %s %d: %d registros\n", nombreMes, anio, total));
            }

        } catch (SQLException e) {
            logger.error("Error al obtener estadísticas de registro", e);
            stats.append("Error al obtener estadísticas");
        }

        return stats.toString();
    }

    // Agregar método auxiliar:
    private String obtenerNombreMes(int mes) {
        String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        return (mes >= 1 && mes <= 12) ? meses[mes - 1] : "Mes " + mes;
    }

    /**
     * Obtiene estadísticas de contacto de padres
     * @return String con estadísticas de información de contacto
     */
    // Si ReporteService usa este método, agregarlo:
    public List<PadreFamilia> findAll() {
        return findActive(); // O usar el findAll() heredado si necesitas todos incluidos inactivos
    }
}
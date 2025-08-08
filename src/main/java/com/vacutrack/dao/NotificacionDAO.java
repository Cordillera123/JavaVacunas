package com.vacutrack.dao;

import com.vacutrack.model.Notificacion;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO para la entidad Notificacion - VERSIÓN CORREGIDA
 * Maneja operaciones de base de datos para notificaciones y alertas del sistema
 * Sistema de recordatorios inteligente para vacunación
 *
 * @author VACU-TRACK Team
 * @version 1.1 - Corregida
 */
public class NotificacionDAO extends BaseDAO<Notificacion, Integer> {

    // Instancia singleton
    private static NotificacionDAO instance;

    // SQL Queries - CORREGIDAS según estructura real de BD
    private static final String INSERT_SQL =
            "INSERT INTO notificaciones (nino_id, vacuna_id, numero_dosis, fecha_programada, " +
                    "tipo_notificacion, estado, mensaje, fecha_envio, fecha_creacion) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE notificaciones SET numero_dosis = ?, fecha_programada = ?, " +
                    "tipo_notificacion = ?, estado = ?, mensaje = ?, fecha_envio = ? " +
                    "WHERE id = ?";

    private static final String FIND_BY_NINO_SQL =
            "SELECT n.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo, " +
                    "ni.nombres as nino_nombres, ni.apellidos as nino_apellidos " +
                    "FROM notificaciones n " +
                    "LEFT JOIN vacunas v ON n.vacuna_id = v.id " +
                    "LEFT JOIN ninos ni ON n.nino_id = ni.id " +
                    "WHERE n.nino_id = ? " +
                    "ORDER BY n.fecha_programada DESC";

    private static final String FIND_BY_ESTADO_SQL =
            "SELECT n.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo, " +
                    "ni.nombres as nino_nombres, ni.apellidos as nino_apellidos " +
                    "FROM notificaciones n " +
                    "LEFT JOIN vacunas v ON n.vacuna_id = v.id " +
                    "LEFT JOIN ninos ni ON n.nino_id = ni.id " +
                    "WHERE n.estado = ? " +
                    "ORDER BY n.fecha_programada DESC";

    private static final String FIND_BY_TIPO_SQL =
            "SELECT n.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo, " +
                    "ni.nombres as nino_nombres, ni.apellidos as nino_apellidos " +
                    "FROM notificaciones n " +
                    "LEFT JOIN vacunas v ON n.vacuna_id = v.id " +
                    "LEFT JOIN ninos ni ON n.nino_id = ni.id " +
                    "WHERE n.tipo_notificacion = ? " +
                    "ORDER BY n.fecha_programada DESC";

    private static final String FIND_PENDIENTES_SQL =
            "SELECT n.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo, " +
                    "ni.nombres as nino_nombres, ni.apellidos as nino_apellidos " +
                    "FROM notificaciones n " +
                    "LEFT JOIN vacunas v ON n.vacuna_id = v.id " +
                    "LEFT JOIN ninos ni ON n.nino_id = ni.id " +
                    "WHERE n.estado = 'PENDIENTE' AND n.fecha_programada <= ? " +
                    "ORDER BY n.fecha_programada ASC";

    private static final String FIND_VENCIDAS_SQL =
            "SELECT n.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo, " +
                    "ni.nombres as nino_nombres, ni.apellidos as nino_apellidos " +
                    "FROM notificaciones n " +
                    "LEFT JOIN vacunas v ON n.vacuna_id = v.id " +
                    "LEFT JOIN ninos ni ON n.nino_id = ni.id " +
                    "WHERE n.tipo_notificacion = 'VENCIDA' " +
                    "ORDER BY n.fecha_programada ASC";

    private static final String FIND_BY_VACUNA_SQL =
            "SELECT n.*, ni.nombres as nino_nombres, ni.apellidos as nino_apellidos " +
                    "FROM notificaciones n " +
                    "LEFT JOIN ninos ni ON n.nino_id = ni.id " +
                    "WHERE n.vacuna_id = ? " +
                    "ORDER BY n.fecha_programada DESC";

    private static final String COUNT_BY_ESTADO_SQL =
            "SELECT COUNT(*) FROM notificaciones WHERE estado = ?";

    private static final String UPDATE_ESTADO_SQL =
            "UPDATE notificaciones SET estado = ?, fecha_envio = ? WHERE id = ?";

    private static final String MARK_AS_READ_SQL =
            "UPDATE notificaciones SET estado = 'LEIDA' WHERE id = ?";

    private static final String FIND_PROXIMAS_SQL =
            "SELECT n.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo, " +
                    "ni.nombres as nino_nombres, ni.apellidos as nino_apellidos " +
                    "FROM notificaciones n " +
                    "LEFT JOIN vacunas v ON n.vacuna_id = v.id " +
                    "LEFT JOIN ninos ni ON n.nino_id = ni.id " +
                    "WHERE n.tipo_notificacion = 'PROXIMA' AND n.estado IN ('PENDIENTE', 'ENVIADA') " +
                    "ORDER BY n.fecha_programada ASC";

    // Estados de notificación (según BD real)
    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_ENVIADA = "ENVIADA";
    public static final String ESTADO_LEIDA = "LEIDA";
    public static final String ESTADO_APLICADA = "APLICADA";

    // Tipos de notificación (según BD real)
    public static final String TIPO_PROXIMA = "PROXIMA";
    public static final String TIPO_VENCIDA = "VENCIDA";
    public static final String TIPO_RECORDATORIO = "RECORDATORIO";

    /**
     * Constructor privado para patrón singleton
     */
    private NotificacionDAO() {
        super();
    }

    /**
     * Obtiene la instancia singleton del DAO
     * @return instancia de NotificacionDAO
     */
    public static synchronized NotificacionDAO getInstance() {
        if (instance == null) {
            instance = new NotificacionDAO();
        }
        return instance;
    }

    @Override
    protected String getTableName() {
        return "notificaciones";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected Notificacion mapResultSetToEntity(ResultSet rs) throws SQLException {
        Notificacion notificacion = new Notificacion();

        notificacion.setId(getInteger(rs, "id"));
        notificacion.setNinoId(getInteger(rs, "nino_id"));
        notificacion.setVacunaId(getInteger(rs, "vacuna_id"));
        notificacion.setNumeroDosis(getInteger(rs, "numero_dosis"));
        notificacion.setFechaProgramada(toLocalDate(rs.getDate("fecha_programada")));
        notificacion.setTipoNotificacion(getString(rs, "tipo_notificacion"));
        notificacion.setEstado(getString(rs, "estado"));
        notificacion.setMensaje(getString(rs, "mensaje"));
        notificacion.setFechaEnvio(toLocalDateTime(rs.getTimestamp("fecha_envio")));
        notificacion.setFechaCreacion(toLocalDateTime(rs.getTimestamp("fecha_creacion")));

        // Mapear información adicional si está disponible
        try {
            String vacunaNombre = getString(rs, "vacuna_nombre");
            if (vacunaNombre != null) {
                notificacion.setVacunaNombre(vacunaNombre);
            }

            String ninoNombres = getString(rs, "nino_nombres");
            String ninoApellidos = getString(rs, "nino_apellidos");
            if (ninoNombres != null && ninoApellidos != null) {
                notificacion.setNinoNombreCompleto(ninoNombres + " " + ninoApellidos);
            }
        } catch (SQLException e) {
            // Información adicional no disponible, ignorar
            logger.debug("Información adicional no disponible en el ResultSet");
        }

        return notificacion;
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Notificacion notificacion) throws SQLException {
        ps.setInt(1, notificacion.getNinoId());
        ps.setInt(2, notificacion.getVacunaId());
        ps.setInt(3, notificacion.getNumeroDosis() != null ? notificacion.getNumeroDosis() : 1);
        ps.setDate(4, java.sql.Date.valueOf(notificacion.getFechaProgramada()));
        ps.setString(5, notificacion.getTipoNotificacion() != null ?
                notificacion.getTipoNotificacion() : TIPO_RECORDATORIO);
        ps.setString(6, notificacion.getEstado() != null ? notificacion.getEstado() : ESTADO_PENDIENTE);
        ps.setString(7, notificacion.getMensaje());

        if (notificacion.getFechaEnvio() != null) {
            ps.setTimestamp(8, java.sql.Timestamp.valueOf(notificacion.getFechaEnvio()));
        } else {
            ps.setNull(8, Types.TIMESTAMP);
        }

        ps.setTimestamp(9, java.sql.Timestamp.valueOf(
                notificacion.getFechaCreacion() != null ? notificacion.getFechaCreacion() : LocalDateTime.now()));
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, Notificacion notificacion) throws SQLException {
        ps.setInt(1, notificacion.getNumeroDosis() != null ? notificacion.getNumeroDosis() : 1);
        ps.setDate(2, java.sql.Date.valueOf(notificacion.getFechaProgramada()));
        ps.setString(3, notificacion.getTipoNotificacion() != null ?
                notificacion.getTipoNotificacion() : TIPO_RECORDATORIO);
        ps.setString(4, notificacion.getEstado() != null ? notificacion.getEstado() : ESTADO_PENDIENTE);
        ps.setString(5, notificacion.getMensaje());

        if (notificacion.getFechaEnvio() != null) {
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(notificacion.getFechaEnvio()));
        } else {
            ps.setNull(6, Types.TIMESTAMP);
        }

        ps.setInt(7, notificacion.getId());
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
    protected void assignGeneratedId(Notificacion notificacion, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            notificacion.setId(generatedKeys.getInt(1));
        }
    }

    // Métodos específicos para notificaciones

    /**
     * Busca notificaciones por niño
     * @param ninoId ID del niño
     * @return lista de notificaciones del niño
     */
    public List<Notificacion> findByNino(Integer ninoId) {
        if (ninoId == null) {
            return List.of();
        }

        logQuery(FIND_BY_NINO_SQL, ninoId);
        List<Notificacion> notificaciones = executeQuery(FIND_BY_NINO_SQL, ninoId);

        logger.debug("Encontradas {} notificaciones para niño ID: {}", notificaciones.size(), ninoId);
        return notificaciones;
    }

    /**
     * Busca notificaciones por estado
     * @param estado estado de la notificación
     * @return lista de notificaciones con el estado especificado
     */
    public List<Notificacion> findByEstado(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            return List.of();
        }

        logQuery(FIND_BY_ESTADO_SQL, estado);
        List<Notificacion> notificaciones = executeQuery(FIND_BY_ESTADO_SQL, estado.trim().toUpperCase());

        logger.debug("Encontradas {} notificaciones con estado '{}'", notificaciones.size(), estado);
        return notificaciones;
    }

    /**
     * Busca notificaciones por tipo
     * @param tipoNotificacion tipo de notificación
     * @return lista de notificaciones del tipo especificado
     */
    public List<Notificacion> findByTipo(String tipoNotificacion) {
        if (tipoNotificacion == null || tipoNotificacion.trim().isEmpty()) {
            return List.of();
        }

        logQuery(FIND_BY_TIPO_SQL, tipoNotificacion);
        List<Notificacion> notificaciones = executeQuery(FIND_BY_TIPO_SQL, tipoNotificacion.trim().toUpperCase());

        logger.debug("Encontradas {} notificaciones de tipo '{}'", notificaciones.size(), tipoNotificacion);
        return notificaciones;
    }

    /**
     * Busca notificaciones pendientes que deben enviarse
     * @param fechaLimite fecha límite para considerar notificaciones
     * @return lista de notificaciones pendientes
     */
    public List<Notificacion> findPendientes(LocalDate fechaLimite) {
        LocalDate limite = fechaLimite != null ? fechaLimite : LocalDate.now();

        logQuery(FIND_PENDIENTES_SQL, limite);
        List<Notificacion> notificaciones = executeQuery(FIND_PENDIENTES_SQL,
                java.sql.Date.valueOf(limite));

        logger.debug("Encontradas {} notificaciones pendientes hasta {}", notificaciones.size(), limite);
        return notificaciones;
    }

    /**
     * Busca notificaciones vencidas
     * @return lista de notificaciones vencidas
     */
    public List<Notificacion> findVencidas() {
        logQuery(FIND_VENCIDAS_SQL);
        List<Notificacion> notificaciones = executeQuery(FIND_VENCIDAS_SQL);

        logger.debug("Encontradas {} notificaciones vencidas", notificaciones.size());
        return notificaciones;
    }

    /**
     * Busca notificaciones por vacuna
     * @param vacunaId ID de la vacuna
     * @return lista de notificaciones de la vacuna
     */
    public List<Notificacion> findByVacuna(Integer vacunaId) {
        if (vacunaId == null) {
            return List.of();
        }

        logQuery(FIND_BY_VACUNA_SQL, vacunaId);
        List<Notificacion> notificaciones = executeQuery(FIND_BY_VACUNA_SQL, vacunaId);

        logger.debug("Encontradas {} notificaciones para vacuna ID: {}", notificaciones.size(), vacunaId);
        return notificaciones;
    }

    /**
     * Busca notificaciones próximas activas
     * @return lista de notificaciones próximas
     */
    public List<Notificacion> findProximas() {
        logQuery(FIND_PROXIMAS_SQL);
        List<Notificacion> notificaciones = executeQuery(FIND_PROXIMAS_SQL);

        logger.debug("Encontradas {} notificaciones próximas", notificaciones.size());
        return notificaciones;
    }

    /**
     * Cuenta notificaciones por estado
     * @param estado estado de la notificación
     * @return número de notificaciones con el estado especificado
     */
    public long countByEstado(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            return 0;
        }

        logQuery(COUNT_BY_ESTADO_SQL, estado);
        Object result = executeScalar(COUNT_BY_ESTADO_SQL, estado.trim().toUpperCase());

        long count = result instanceof Number ? ((Number) result).longValue() : 0;
        logger.debug("Conteo de notificaciones con estado '{}': {}", estado, count);

        return count;
    }

    /**
     * Actualiza el estado de una notificación
     * @param notificacionId ID de la notificación
     * @param nuevoEstado nuevo estado
     * @return true si se actualizó correctamente
     */
    public boolean updateEstado(Integer notificacionId, String nuevoEstado) {
        if (notificacionId == null || nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            return false;
        }

        try {
            LocalDateTime fechaEnvio = ESTADO_ENVIADA.equals(nuevoEstado.trim().toUpperCase()) ?
                    LocalDateTime.now() : null;

            logQuery(UPDATE_ESTADO_SQL, nuevoEstado, fechaEnvio, notificacionId);
            int rowsAffected = executeUpdate(UPDATE_ESTADO_SQL,
                    nuevoEstado.trim().toUpperCase(),
                    fechaEnvio != null ? java.sql.Timestamp.valueOf(fechaEnvio) : null,
                    notificacionId);

            boolean updated = rowsAffected > 0;
            logger.debug("Estado actualizado para notificación {}: {}", notificacionId, updated);

            return updated;
        } catch (Exception e) {
            logger.error("Error al actualizar estado de la notificación " + notificacionId, e);
            return false;
        }
    }

    /**
     * Marca una notificación como leída
     * @param notificacionId ID de la notificación
     * @return true si se marcó correctamente
     */
    public boolean markAsRead(Integer notificacionId) {
        if (notificacionId == null) {
            return false;
        }

        try {
            logQuery(MARK_AS_READ_SQL, notificacionId);
            int rowsAffected = executeUpdate(MARK_AS_READ_SQL, notificacionId);

            boolean marked = rowsAffected > 0;
            logger.debug("Notificación marcada como leída {}: {}", notificacionId, marked);

            return marked;
        } catch (Exception e) {
            logger.error("Error al marcar notificación como leída " + notificacionId, e);
            return false;
        }
    }

    /**
     * SIMPLIFICADO: Crea una notificación de recordatorio de vacuna
     * @param ninoId ID del niño
     * @param vacunaId ID de la vacuna
     * @param numeroDosis número de dosis
     * @param fechaProgramada fecha programada para la vacuna
     * @return notificación creada o null si hubo error
     */
    public Notificacion crearRecordatorioVacuna(Integer ninoId, Integer vacunaId, Integer numeroDosis,
                                                LocalDate fechaProgramada) {
        if (ninoId == null || vacunaId == null || fechaProgramada == null) {
            return null;
        }

        try {
            Notificacion notificacion = new Notificacion();
            notificacion.setNinoId(ninoId);
            notificacion.setVacunaId(vacunaId);
            notificacion.setNumeroDosis(numeroDosis != null ? numeroDosis : 1);
            notificacion.setFechaProgramada(fechaProgramada);
            notificacion.setTipoNotificacion(TIPO_RECORDATORIO);
            notificacion.setEstado(ESTADO_PENDIENTE);
            notificacion.setMensaje("Recordatorio de vacunación programada para el " + fechaProgramada);
            notificacion.setFechaCreacion(LocalDateTime.now());

            if (save(notificacion)) {
                logger.info("Recordatorio de vacuna creado para niño {} - vacuna {}", ninoId, vacunaId);
                return notificacion;
            }

        } catch (Exception e) {
            logger.error("Error al crear recordatorio de vacuna", e);
        }

        return null;
    }

    /**
     * SIMPLIFICADO: Crea una notificación de vacuna vencida
     * @param ninoId ID del niño
     * @param vacunaId ID de la vacuna
     * @param numeroDosis número de dosis
     * @param fechaVencida fecha que ya pasó
     * @return notificación creada o null si hubo error
     */
    public Notificacion crearAlertaVacunaVencida(Integer ninoId, Integer vacunaId, Integer numeroDosis,
                                                 LocalDate fechaVencida) {
        if (ninoId == null || vacunaId == null || fechaVencida == null) {
            return null;
        }

        try {
            Notificacion notificacion = new Notificacion();
            notificacion.setNinoId(ninoId);
            notificacion.setVacunaId(vacunaId);
            notificacion.setNumeroDosis(numeroDosis != null ? numeroDosis : 1);
            notificacion.setFechaProgramada(fechaVencida);
            notificacion.setTipoNotificacion(TIPO_VENCIDA);
            notificacion.setEstado(ESTADO_PENDIENTE);
            notificacion.setMensaje("¡VACUNA VENCIDA! Debió aplicarse el " + fechaVencida +
                    ". Contacte a su centro de salud.");
            notificacion.setFechaCreacion(LocalDateTime.now());

            if (save(notificacion)) {
                logger.info("Alerta de vacuna vencida creada para niño {} - vacuna {}", ninoId, vacunaId);
                return notificacion;
            }

        } catch (Exception e) {
            logger.error("Error al crear alerta de vacuna vencida", e);
        }

        return null;
    }

    /**
     * Crea notificación próxima
     * @param ninoId ID del niño
     * @param vacunaId ID de la vacuna
     * @param numeroDosis número de dosis
     * @param fechaProgramada fecha programada
     * @return notificación creada o null si hubo error
     */
    public Notificacion crearNotificacionProxima(Integer ninoId, Integer vacunaId, Integer numeroDosis,
                                                 LocalDate fechaProgramada) {
        if (ninoId == null || vacunaId == null || fechaProgramada == null) {
            return null;
        }

        try {
            Notificacion notificacion = new Notificacion();
            notificacion.setNinoId(ninoId);
            notificacion.setVacunaId(vacunaId);
            notificacion.setNumeroDosis(numeroDosis != null ? numeroDosis : 1);
            notificacion.setFechaProgramada(fechaProgramada);
            notificacion.setTipoNotificacion(TIPO_PROXIMA);
            notificacion.setEstado(ESTADO_PENDIENTE);
            notificacion.setMensaje("Próxima vacuna programada para el " + fechaProgramada);
            notificacion.setFechaCreacion(LocalDateTime.now());

            if (save(notificacion)) {
                logger.info("Notificación próxima creada para niño {} - vacuna {}", ninoId, vacunaId);
                return notificacion;
            }

        } catch (Exception e) {
            logger.error("Error al crear notificación próxima", e);
        }

        return null;
    }

    /**
     * Obtiene notificaciones activas (no leídas ni aplicadas)
     * @param ninoId ID del niño
     * @return lista de notificaciones activas
     */
    public List<Notificacion> getNotificacionesActivas(Integer ninoId) {
        if (ninoId == null) {
            return List.of();
        }

        List<Notificacion> todas = findByNino(ninoId);
        return todas.stream()
                .filter(n -> ESTADO_PENDIENTE.equals(n.getEstado()) || ESTADO_ENVIADA.equals(n.getEstado()))
                .toList();
    }

    /**
     * Marca notificación como aplicada (vacuna completada)
     * @param notificacionId ID de la notificación
     * @return true si se marcó correctamente
     */
    public boolean markAsAplicada(Integer notificacionId) {
        return updateEstado(notificacionId, ESTADO_APLICADA);
    }

    /**
     * Busca notificaciones que necesitan ser enviadas hoy
     * @return lista de notificaciones para enviar
     */
    public List<Notificacion> findParaEnviarHoy() {
        return findPendientes(LocalDate.now());
    }

    /**
     * Verifica si existe una notificación para una vacuna específica
     * @param ninoId ID del niño
     * @param vacunaId ID de la vacuna
     * @param numeroDosis número de dosis
     * @return true si existe la notificación
     */
    public boolean existsNotificacion(Integer ninoId, Integer vacunaId, Integer numeroDosis) {
        if (ninoId == null || vacunaId == null) {
            return false;
        }

        String sql = "SELECT 1 FROM notificaciones " +
                "WHERE nino_id = ? AND vacuna_id = ? AND numero_dosis = ? " +
                "LIMIT 1";

        Object result = executeScalar(sql, ninoId, vacunaId,
                numeroDosis != null ? numeroDosis : 1);
        return result != null;
    }

    /**
     * Elimina notificaciones antiguas aplicadas
     * @param diasAntiguedad días de antigüedad mínima
     * @return número de notificaciones eliminadas
     */
    public int cleanupNotificacionesAplicadas(int diasAntiguedad) {
        if (diasAntiguedad <= 0) {
            diasAntiguedad = 90; // 3 meses por defecto
        }

        String sql = "DELETE FROM notificaciones " +
                "WHERE estado = 'APLICADA' AND fecha_creacion < DATE_SUB(CURDATE(), INTERVAL ? DAY)";

        try {
            int deleted = executeUpdate(sql, diasAntiguedad);
            logger.info("Eliminadas {} notificaciones aplicadas antiguas", deleted);
            return deleted;
        } catch (Exception e) {
            logger.error("Error al limpiar notificaciones aplicadas", e);
            return 0;
        }
    }

    /**
     * CORREGIDO: Obtiene estadísticas de notificaciones (solo con campos reales)
     * @return String con estadísticas formateadas
     */
    public String getNotificacionStatistics() {
        StringBuilder stats = new StringBuilder();

        try {
            long totalNotificaciones = count();
            long pendientes = countByEstado(ESTADO_PENDIENTE);
            long enviadas = countByEstado(ESTADO_ENVIADA);
            long leidas = countByEstado(ESTADO_LEIDA);
            long aplicadas = countByEstado(ESTADO_APLICADA);

            long proximas = findByTipo(TIPO_PROXIMA).size();
            long recordatorios = findByTipo(TIPO_RECORDATORIO).size();
            long vencidas = findByTipo(TIPO_VENCIDA).size();

            stats.append("ESTADÍSTICAS DE NOTIFICACIONES\n");
            stats.append("==============================\n");
            stats.append("Total notificaciones: ").append(totalNotificaciones).append("\n");

            stats.append("\nPOR ESTADO\n");
            stats.append("==========\n");
            stats.append("Pendientes: ").append(pendientes).append("\n");
            stats.append("Enviadas: ").append(enviadas).append("\n");
            stats.append("Leídas: ").append(leidas).append("\n");
            stats.append("Aplicadas: ").append(aplicadas).append("\n");

            stats.append("\nPOR TIPO\n");
            stats.append("========\n");
            stats.append("Próximas: ").append(proximas).append("\n");
            stats.append("Recordatorios: ").append(recordatorios).append("\n");
            stats.append("Vencidas: ").append(vencidas).append("\n");

        } catch (Exception e) {
            logger.error("Error al obtener estadísticas de notificaciones", e);
            stats.append("Error al generar estadísticas: ").append(e.getMessage());
        }

        return stats.toString();
    }

    /**
     * Busca notificaciones por fecha programada
     * @param fechaProgramada fecha específica
     * @return lista de notificaciones para esa fecha
     */
    public List<Notificacion> findByFechaProgramada(LocalDate fechaProgramada) {
        if (fechaProgramada == null) {
            return List.of();
        }

        String sql = "SELECT n.*, v.nombre as vacuna_nombre, " +
                "ni.nombres as nino_nombres, ni.apellidos as nino_apellidos " +
                "FROM notificaciones n " +
                "LEFT JOIN vacunas v ON n.vacuna_id = v.id " +
                "LEFT JOIN ninos ni ON n.nino_id = ni.id " +
                "WHERE n.fecha_programada = ? " +
                "ORDER BY ni.nombres, ni.apellidos";

        logQuery(sql, fechaProgramada);
        List<Notificacion> notificaciones = executeQuery(sql, java.sql.Date.valueOf(fechaProgramada));

        logger.debug("Encontradas {} notificaciones para fecha {}", notificaciones.size(), fechaProgramada);
        return notificaciones;
    }

    /**
     * Busca notificaciones en un rango de fechas
     * @param fechaInicio fecha de inicio
     * @param fechaFin fecha de fin
     * @return lista de notificaciones en el rango
     */
    public List<Notificacion> findByFechaRange(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return List.of();
        }

        String sql = "SELECT n.*, v.nombre as vacuna_nombre, " +
                "ni.nombres as nino_nombres, ni.apellidos as nino_apellidos " +
                "FROM notificaciones n " +
                "LEFT JOIN vacunas v ON n.vacuna_id = v.id " +
                "LEFT JOIN ninos ni ON n.nino_id = ni.id " +
                "WHERE n.fecha_programada BETWEEN ? AND ? " +
                "ORDER BY n.fecha_programada, ni.nombres";

        logQuery(sql, fechaInicio, fechaFin);
        List<Notificacion> notificaciones = executeQuery(sql,
                java.sql.Date.valueOf(fechaInicio), java.sql.Date.valueOf(fechaFin));

        logger.debug("Encontradas {} notificaciones entre {} y {}",
                notificaciones.size(), fechaInicio, fechaFin);
        return notificaciones;
    }

    /**
     * Obtiene las notificaciones más recientes
     * @param limite número máximo de notificaciones
     * @return lista de notificaciones recientes
     */
    public List<Notificacion> findRecientes(int limite) {
        if (limite <= 0) {
            limite = 10;
        }

        String sql = "SELECT n.*, v.nombre as vacuna_nombre, " +
                "ni.nombres as nino_nombres, ni.apellidos as nino_apellidos " +
                "FROM notificaciones n " +
                "LEFT JOIN vacunas v ON n.vacuna_id = v.id " +
                "LEFT JOIN ninos ni ON n.nino_id = ni.id " +
                "ORDER BY n.fecha_creacion DESC " +
                "LIMIT ?";

        logQuery(sql, limite);
        List<Notificacion> notificaciones = executeQuery(sql, limite);

        logger.debug("Encontradas {} notificaciones recientes", notificaciones.size());
        return notificaciones;
    }

    /**
     * Actualiza el mensaje de una notificación
     * @param notificacionId ID de la notificación
     * @param nuevoMensaje nuevo mensaje
     * @return true si se actualizó correctamente
     */
    public boolean updateMensaje(Integer notificacionId, String nuevoMensaje) {
        if (notificacionId == null || nuevoMensaje == null) {
            return false;
        }

        String sql = "UPDATE notificaciones SET mensaje = ? WHERE id = ?";

        try {
            int rowsAffected = executeUpdate(sql, nuevoMensaje.trim(), notificacionId);
            boolean updated = rowsAffected > 0;

            if (updated) {
                logger.debug("Mensaje actualizado para notificación ID: {}", notificacionId);
            }

            return updated;
        } catch (Exception e) {
            logger.error("Error al actualizar mensaje de notificación " + notificacionId, e);
            return false;
        }
    }

    /**
     * Actualiza la fecha programada de una notificación
     * @param notificacionId ID de la notificación
     * @param nuevaFecha nueva fecha programada
     * @return true si se actualizó correctamente
     */
    public boolean updateFechaProgramada(Integer notificacionId, LocalDate nuevaFecha) {
        if (notificacionId == null || nuevaFecha == null) {
            return false;
        }

        String sql = "UPDATE notificaciones SET fecha_programada = ? WHERE id = ?";

        try {
            int rowsAffected = executeUpdate(sql, java.sql.Date.valueOf(nuevaFecha), notificacionId);
            boolean updated = rowsAffected > 0;

            if (updated) {
                logger.debug("Fecha programada actualizada para notificación ID: {} -> {}",
                        notificacionId, nuevaFecha);
            }

            return updated;
        } catch (Exception e) {
            logger.error("Error al actualizar fecha de notificación " + notificacionId, e);
            return false;
        }
    }

    /**
     * Cuenta notificaciones por niño y estado
     * @param ninoId ID del niño
     * @param estado estado a contar
     * @return número de notificaciones
     */
    public long countByNinoAndEstado(Integer ninoId, String estado) {
        if (ninoId == null || estado == null || estado.trim().isEmpty()) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM notificaciones WHERE nino_id = ? AND estado = ?";
        Object result = executeScalar(sql, ninoId, estado.trim().toUpperCase());

        long count = result instanceof Number ? ((Number) result).longValue() : 0;
        logger.debug("Conteo de notificaciones para niño {} con estado '{}': {}", ninoId, estado, count);

        return count;
    }

    /**
     * Busca notificaciones duplicadas (misma vacuna, dosis y fecha para el mismo niño)
     * @return lista de notificaciones que podrían ser duplicadas
     */
    public List<Notificacion> findPosiblesDuplicadas() {
        String sql = "SELECT n1.*, v.nombre as vacuna_nombre, " +
                "ni.nombres as nino_nombres, ni.apellidos as nino_apellidos " +
                "FROM notificaciones n1 " +
                "INNER JOIN notificaciones n2 ON n1.nino_id = n2.nino_id " +
                "AND n1.vacuna_id = n2.vacuna_id " +
                "AND n1.numero_dosis = n2.numero_dosis " +
                "AND n1.fecha_programada = n2.fecha_programada " +
                "AND n1.id < n2.id " +
                "LEFT JOIN vacunas v ON n1.vacuna_id = v.id " +
                "LEFT JOIN ninos ni ON n1.nino_id = ni.id " +
                "ORDER BY n1.nino_id, n1.vacuna_id, n1.fecha_programada";

        logQuery(sql);
        List<Notificacion> duplicadas = executeQuery(sql);

        logger.debug("Encontradas {} posibles notificaciones duplicadas", duplicadas.size());
        return duplicadas;
    }

    /**
     * Elimina notificación por ID
     * @param notificacionId ID de la notificación a eliminar
     * @return true si se eliminó correctamente
     */
    public boolean deleteNotificacion(Integer notificacionId) {
        if (notificacionId == null) {
            return false;
        }

        return deleteById(notificacionId);
    }

    /**
     * Busca notificaciones activas ordenadas por prioridad (tipo)
     * @return lista de notificaciones ordenadas por importancia
     */
    public List<Notificacion> findActivasOrdenadaPorPrioridad() {
        String sql = "SELECT n.*, v.nombre as vacuna_nombre, " +
                "ni.nombres as nino_nombres, ni.apellidos as nino_apellidos " +
                "FROM notificaciones n " +
                "LEFT JOIN vacunas v ON n.vacuna_id = v.id " +
                "LEFT JOIN ninos ni ON n.nino_id = ni.id " +
                "WHERE n.estado IN ('PENDIENTE', 'ENVIADA') " +
                "ORDER BY " +
                "CASE n.tipo_notificacion " +
                "WHEN 'VENCIDA' THEN 1 " +
                "WHEN 'PROXIMA' THEN 2 " +
                "WHEN 'RECORDATORIO' THEN 3 " +
                "ELSE 4 END, " +
                "n.fecha_programada ASC";

        logQuery(sql);
        List<Notificacion> notificaciones = executeQuery(sql);

        logger.debug("Encontradas {} notificaciones activas ordenadas por prioridad",
                notificaciones.size());
        return notificaciones;
    }

    /**
     * Resumen de notificaciones por niño
     * @param ninoId ID del niño
     * @return String con resumen formateado
     */
    public String getResumenPorNino(Integer ninoId) {
        if (ninoId == null) {
            return "ID de niño no válido";
        }

        List<Notificacion> notificaciones = findByNino(ninoId);

        if (notificaciones.isEmpty()) {
            return "No hay notificaciones para este niño";
        }

        StringBuilder resumen = new StringBuilder();
        String nombreNino = notificaciones.get(0).getNinoNombreCompleto();

        resumen.append("NOTIFICACIONES PARA: ").append(nombreNino != null ? nombreNino : "Niño ID " + ninoId).append("\n");
        resumen.append("=".repeat(50)).append("\n");

        long pendientes = notificaciones.stream().filter(n -> ESTADO_PENDIENTE.equals(n.getEstado())).count();
        long enviadas = notificaciones.stream().filter(n -> ESTADO_ENVIADA.equals(n.getEstado())).count();
        long leidas = notificaciones.stream().filter(n -> ESTADO_LEIDA.equals(n.getEstado())).count();
        long aplicadas = notificaciones.stream().filter(n -> ESTADO_APLICADA.equals(n.getEstado())).count();

        resumen.append("Total: ").append(notificaciones.size()).append(" notificaciones\n");
        resumen.append("Pendientes: ").append(pendientes).append("\n");
        resumen.append("Enviadas: ").append(enviadas).append("\n");
        resumen.append("Leídas: ").append(leidas).append("\n");
        resumen.append("Aplicadas: ").append(aplicadas).append("\n\n");

        // Mostrar las 5 más recientes
        resumen.append("ÚLTIMAS NOTIFICACIONES:\n");
        notificaciones.stream()
                .limit(5)
                .forEach(n -> {
                    resumen.append("• ").append(n.getFechaProgramada())
                            .append(" - ").append(n.getVacunaNombre() != null ? n.getVacunaNombre() : "Vacuna")
                            .append(" (Dosis ").append(n.getNumeroDosis()).append(")")
                            .append(" - ").append(n.getEstado()).append("\n");
                });

        return resumen.toString();
    }

    /**
     * Procesa notificaciones automáticas basadas en la fecha actual
     * Actualiza tipos de notificación según si están vencidas, próximas, etc.
     * @return número de notificaciones procesadas
     */
    public int procesarNotificacionesAutomaticas() {
        LocalDate hoy = LocalDate.now();
        int procesadas = 0;

        try {
            // Buscar notificaciones que pueden cambiar de tipo
            String sql = "SELECT * FROM notificaciones WHERE estado IN ('PENDIENTE', 'ENVIADA')";
            List<Notificacion> notificaciones = executeQuery(sql);

            for (Notificacion notificacion : notificaciones) {
                LocalDate fechaProgramada = notificacion.getFechaProgramada();
                if (fechaProgramada == null) continue;

                String tipoActual = notificacion.getTipoNotificacion();
                String nuevoTipo = null;

                // Determinar nuevo tipo según la fecha
                if (fechaProgramada.isBefore(hoy)) {
                    // Ya pasó la fecha - es vencida
                    if (!TIPO_VENCIDA.equals(tipoActual)) {
                        nuevoTipo = TIPO_VENCIDA;
                    }
                } else if (fechaProgramada.equals(hoy) || fechaProgramada.equals(hoy.plusDays(1))) {
                    // Es hoy o mañana - es próxima
                    if (!TIPO_PROXIMA.equals(tipoActual)) {
                        nuevoTipo = TIPO_PROXIMA;
                    }
                } else if (fechaProgramada.isAfter(hoy.plusDays(1))) {
                    // Aún falta tiempo - es recordatorio
                    if (!TIPO_RECORDATORIO.equals(tipoActual)) {
                        nuevoTipo = TIPO_RECORDATORIO;
                    }
                }

                // Actualizar si es necesario
                if (nuevoTipo != null) {
                    String updateSql = "UPDATE notificaciones SET tipo_notificacion = ? WHERE id = ?";
                    if (executeUpdate(updateSql, nuevoTipo, notificacion.getId()) > 0) {
                        procesadas++;
                        logger.debug("Notificación {} actualizada de {} a {}",
                                notificacion.getId(), tipoActual, nuevoTipo);
                    }
                }
            }

            if (procesadas > 0) {
                logger.info("Procesadas automáticamente {} notificaciones", procesadas);
            }

        } catch (Exception e) {
            logger.error("Error al procesar notificaciones automáticas", e);
        }

        return procesadas;
    }
}
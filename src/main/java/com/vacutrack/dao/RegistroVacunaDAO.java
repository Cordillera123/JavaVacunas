package com.vacutrack.dao;

import com.vacutrack.model.RegistroVacuna;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DAO para la entidad RegistroVacuna
 * Maneja operaciones de base de datos para registros de vacunación
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class RegistroVacunaDAO extends BaseDAO<RegistroVacuna, Integer> {

    // Instancia singleton
    private static RegistroVacunaDAO instance;

    // SQL Queries
    private static final String INSERT_SQL =
            "INSERT INTO registro_vacunas (nino_id, vacuna_id, profesional_id, centro_salud_id, " +
                    "fecha_aplicacion, numero_dosis, lote_vacuna, fecha_vencimiento, observaciones, " +
                    "reaccion_adversa, descripcion_reaccion, peso_aplicacion, talla_aplicacion, " +
                    "fecha_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE registro_vacunas SET vacuna_id = ?, profesional_id = ?, centro_salud_id = ?, " +
                    "fecha_aplicacion = ?, numero_dosis = ?, lote_vacuna = ?, fecha_vencimiento = ?, " +
                    "observaciones = ?, reaccion_adversa = ?, descripcion_reaccion = ?, " +
                    "peso_aplicacion = ?, talla_aplicacion = ? WHERE id = ?";

    private static final String FIND_BY_NINO_SQL =
            "SELECT rv.*, v.nombre as vacuna_nombre, v.descripcion as vacuna_descripcion, " +
                    "v.codigo as vacuna_codigo " +
                    "FROM registro_vacunas rv " +
                    "LEFT JOIN vacunas v ON rv.vacuna_id = v.id " +
                    "WHERE rv.nino_id = ? " +
                    "ORDER BY rv.fecha_aplicacion DESC";

    private static final String FIND_BY_VACUNA_SQL =
            "SELECT rv.*, v.nombre as vacuna_nombre, v.descripcion as vacuna_descripcion, " +
                    "v.codigo as vacuna_codigo " +
                    "FROM registro_vacunas rv " +
                    "LEFT JOIN vacunas v ON rv.vacuna_id = v.id " +
                    "WHERE rv.vacuna_id = ? " +
                    "ORDER BY rv.fecha_aplicacion DESC";

    private static final String FIND_BY_PROFESIONAL_SQL =
            "SELECT rv.*, v.nombre as vacuna_nombre, v.descripcion as vacuna_descripcion, " +
                    "v.codigo as vacuna_codigo " +
                    "FROM registro_vacunas rv " +
                    "LEFT JOIN vacunas v ON rv.vacuna_id = v.id " +
                    "WHERE rv.profesional_id = ? " +
                    "ORDER BY rv.fecha_aplicacion DESC";

    private static final String FIND_BY_FECHA_RANGE_SQL =
            "SELECT rv.*, v.nombre as vacuna_nombre, v.descripcion as vacuna_descripcion, " +
                    "v.codigo as vacuna_codigo " +
                    "FROM registro_vacunas rv " +
                    "LEFT JOIN vacunas v ON rv.vacuna_id = v.id " +
                    "WHERE rv.fecha_aplicacion BETWEEN ? AND ? " +
                    "ORDER BY rv.fecha_aplicacion DESC";

    private static final String FIND_BY_NINO_AND_VACUNA_SQL =
            "SELECT rv.*, v.nombre as vacuna_nombre, v.descripcion as vacuna_descripcion, " +
                    "v.codigo as vacuna_codigo " +
                    "FROM registro_vacunas rv " +
                    "LEFT JOIN vacunas v ON rv.vacuna_id = v.id " +
                    "WHERE rv.nino_id = ? AND rv.vacuna_id = ? " +
                    "ORDER BY rv.numero_dosis DESC";

    private static final String COUNT_BY_VACUNA_SQL =
            "SELECT COUNT(*) FROM registro_vacunas WHERE vacuna_id = ?";

    private static final String FIND_BY_CENTRO_SALUD_SQL =
            "SELECT rv.*, v.nombre as vacuna_nombre, v.descripcion as vacuna_descripcion, " +
                    "v.codigo as vacuna_codigo " +
                    "FROM registro_vacunas rv " +
                    "LEFT JOIN vacunas v ON rv.vacuna_id = v.id " +
                    "WHERE rv.centro_salud_id = ? " +
                    "ORDER BY rv.fecha_aplicacion DESC";

    // Eliminar las constantes de estado ya que no están en la BD
    // La tabla solo almacena los registros aplicados

    /**
     * Constructor privado para patrón singleton
     */
    private RegistroVacunaDAO() {
        super();
    }

    /**
     * Obtiene la instancia singleton del DAO
     * @return instancia de RegistroVacunaDAO
     */
    public static synchronized RegistroVacunaDAO getInstance() {
        if (instance == null) {
            instance = new RegistroVacunaDAO();
        }
        return instance;
    }

    @Override
    protected String getTableName() {
        return "registro_vacunas";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected RegistroVacuna mapResultSetToEntity(ResultSet rs) throws SQLException {
        RegistroVacuna registro = new RegistroVacuna();

        registro.setId(getInteger(rs, "id"));
        registro.setNinoId(getInteger(rs, "nino_id"));
        registro.setVacunaId(getInteger(rs, "vacuna_id"));
        registro.setProfesionalId(getInteger(rs, "profesional_id"));
        registro.setCentroSaludId(getInteger(rs, "centro_salud_id"));
        registro.setFechaAplicacion(toLocalDate(rs.getDate("fecha_aplicacion")));
        registro.setNumeroDosis(getInteger(rs, "numero_dosis"));
        registro.setLoteVacuna(getString(rs, "lote_vacuna"));
        registro.setFechaVencimiento(toLocalDate(rs.getDate("fecha_vencimiento")));
        registro.setObservaciones(getString(rs, "observaciones"));
        registro.setReaccionAdversa(getBoolean(rs, "reaccion_adversa"));
        registro.setDescripcionReaccion(getString(rs, "descripcion_reaccion"));
        registro.setPesoAplicacion(getBigDecimal(rs, "peso_aplicacion"));
        registro.setTallaAplicacion(getBigDecimal(rs, "talla_aplicacion"));
        registro.setFechaRegistro(toLocalDateTime(rs.getTimestamp("fecha_registro")));

        // Mapear información de vacuna si está disponible
        try {
            String vacunaNombre = getString(rs, "vacuna_nombre");
            if (vacunaNombre != null) {
                registro.setVacunaNombre(vacunaNombre);
                registro.setVacunaDescripcion(getString(rs, "vacuna_descripcion"));
                registro.setVacunaCodigo(getString(rs, "vacuna_codigo"));
            }
        } catch (SQLException e) {
            // Información de vacuna no disponible, ignorar
            logger.debug("Información de vacuna no disponible en el ResultSet");
        }

        return registro;
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, RegistroVacuna registro) throws SQLException {
        ps.setInt(1, registro.getNinoId());
        ps.setInt(2, registro.getVacunaId());
        ps.setObject(3, registro.getProfesionalId(), java.sql.Types.INTEGER);
        ps.setObject(4, registro.getCentroSaludId(), java.sql.Types.INTEGER);
        ps.setDate(5, registro.getFechaAplicacion() != null ?
                java.sql.Date.valueOf(registro.getFechaAplicacion()) : null);
        ps.setInt(6, registro.getNumeroDosis() != null ? registro.getNumeroDosis() : 1);
        ps.setString(7, registro.getLoteVacuna());
        ps.setDate(8, registro.getFechaVencimiento() != null ?
                java.sql.Date.valueOf(registro.getFechaVencimiento()) : null);
        ps.setString(9, registro.getObservaciones());
        ps.setBoolean(10, registro.getReaccionAdversa() != null ? registro.getReaccionAdversa() : false);
        ps.setString(11, registro.getDescripcionReaccion());
        ps.setBigDecimal(12, registro.getPesoAplicacion());
        ps.setBigDecimal(13, registro.getTallaAplicacion());
        ps.setTimestamp(14, java.sql.Timestamp.valueOf(
                registro.getFechaRegistro() != null ? registro.getFechaRegistro() : LocalDateTime.now()));
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, RegistroVacuna registro) throws SQLException {
        ps.setInt(1, registro.getVacunaId());
        ps.setObject(2, registro.getProfesionalId(), java.sql.Types.INTEGER);
        ps.setObject(3, registro.getCentroSaludId(), java.sql.Types.INTEGER);
        ps.setDate(4, registro.getFechaAplicacion() != null ?
                java.sql.Date.valueOf(registro.getFechaAplicacion()) : null);
        ps.setInt(5, registro.getNumeroDosis() != null ? registro.getNumeroDosis() : 1);
        ps.setString(6, registro.getLoteVacuna());
        ps.setDate(7, registro.getFechaVencimiento() != null ?
                java.sql.Date.valueOf(registro.getFechaVencimiento()) : null);
        ps.setString(8, registro.getObservaciones());
        ps.setBoolean(9, registro.getReaccionAdversa() != null ? registro.getReaccionAdversa() : false);
        ps.setString(10, registro.getDescripcionReaccion());
        ps.setBigDecimal(11, registro.getPesoAplicacion());
        ps.setBigDecimal(12, registro.getTallaAplicacion());
        ps.setInt(13, registro.getId());
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
    protected void assignGeneratedId(RegistroVacuna registro, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            registro.setId(generatedKeys.getInt(1));
        }
    }

    // Métodos específicos para registros de vacuna

    /**
     * Busca registros de vacuna por niño
     * @param ninoId ID del niño
     * @return lista de registros del niño
     */
    public List<RegistroVacuna> findByNino(Integer ninoId) {
        if (ninoId == null) {
            return List.of();
        }

        logQuery(FIND_BY_NINO_SQL, ninoId);
        List<RegistroVacuna> registros = executeQuery(FIND_BY_NINO_SQL, ninoId);

        logger.debug("Encontrados {} registros para niño ID: {}", registros.size(), ninoId);
        return registros;
    }

    /**
     * Busca registros por tipo de vacuna
     * @param vacunaId ID de la vacuna
     * @return lista de registros de la vacuna
     */
    public List<RegistroVacuna> findByVacuna(Integer vacunaId) {
        if (vacunaId == null) {
            return List.of();
        }

        logQuery(FIND_BY_VACUNA_SQL, vacunaId);
        List<RegistroVacuna> registros = executeQuery(FIND_BY_VACUNA_SQL, vacunaId);

        logger.debug("Encontrados {} registros para vacuna ID: {}", registros.size(), vacunaId);
        return registros;
    }

    /**
     * Busca registros por centro de salud
     * @param centroSaludId ID del centro de salud
     * @return lista de registros del centro de salud
     */
    public List<RegistroVacuna> findByCentroSalud(Integer centroSaludId) {
        if (centroSaludId == null) {
            return List.of();
        }

        logQuery(FIND_BY_CENTRO_SALUD_SQL, centroSaludId);
        List<RegistroVacuna> registros = executeQuery(FIND_BY_CENTRO_SALUD_SQL, centroSaludId);

        logger.debug("Encontrados {} registros para centro de salud ID: {}", registros.size(), centroSaludId);
        return registros;
    }

    /**
     * Busca registros por profesional
     * @param profesionalId ID del profesional
     * @return lista de registros del profesional
     */
    public List<RegistroVacuna> findByProfesional(Integer profesionalId) {
        if (profesionalId == null) {
            return List.of();
        }

        logQuery(FIND_BY_PROFESIONAL_SQL, profesionalId);
        List<RegistroVacuna> registros = executeQuery(FIND_BY_PROFESIONAL_SQL, profesionalId);

        logger.debug("Encontrados {} registros para profesional ID: {}", registros.size(), profesionalId);
        return registros;
    }

    /**
     * Busca registros por rango de fechas
     * @param fechaInicio fecha de inicio
     * @param fechaFin fecha de fin
     * @return lista de registros en el rango de fechas
     */
    public List<RegistroVacuna> findByFechaRange(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return List.of();
        }

        logQuery(FIND_BY_FECHA_RANGE_SQL, fechaInicio, fechaFin);
        List<RegistroVacuna> registros = executeQuery(FIND_BY_FECHA_RANGE_SQL,
                java.sql.Date.valueOf(fechaInicio), java.sql.Date.valueOf(fechaFin));

        logger.debug("Encontrados {} registros entre {} y {}", registros.size(), fechaInicio, fechaFin);
        return registros;
    }

    /**
     * Busca registros con reacciones adversas
     * @return lista de registros con reacciones adversas
     */
    public List<RegistroVacuna> findWithReaccionesAdversas() {
        String sql = "SELECT rv.*, v.nombre as vacuna_nombre, v.descripcion as vacuna_descripcion, " +
                "v.codigo as vacuna_codigo " +
                "FROM registro_vacunas rv " +
                "LEFT JOIN vacunas v ON rv.vacuna_id = v.id " +
                "WHERE rv.reaccion_adversa = true " +
                "ORDER BY rv.fecha_aplicacion DESC";

        logQuery(sql);
        List<RegistroVacuna> registros = executeQuery(sql);

        logger.debug("Encontrados {} registros con reacciones adversas", registros.size());
        return registros;
    }

    /**
     * Busca registros de un niño para una vacuna específica
     * @param ninoId ID del niño
     * @param vacunaId ID de la vacuna
     * @return lista de registros del niño para la vacuna
     */
    public List<RegistroVacuna> findByNinoAndVacuna(Integer ninoId, Integer vacunaId) {
        if (ninoId == null || vacunaId == null) {
            return List.of();
        }

        logQuery(FIND_BY_NINO_AND_VACUNA_SQL, ninoId, vacunaId);
        List<RegistroVacuna> registros = executeQuery(FIND_BY_NINO_AND_VACUNA_SQL, ninoId, vacunaId);

        logger.debug("Encontrados {} registros para niño {} y vacuna {}", registros.size(), ninoId, vacunaId);
        return registros;
    }

    /**
     * Cuenta registros por vacuna
     * @param vacunaId ID de la vacuna
     * @return número de registros de la vacuna
     */
    public long countByVacuna(Integer vacunaId) {
        if (vacunaId == null) {
            return 0;
        }

        logQuery(COUNT_BY_VACUNA_SQL, vacunaId);
        Object result = executeScalar(COUNT_BY_VACUNA_SQL, vacunaId);

        long count = result instanceof Number ? ((Number) result).longValue() : 0;
        logger.debug("Conteo de registros para vacuna ID {}: {}", vacunaId, count);

        return count;
    }

    /**
     * Cuenta registros con reacciones adversas
     * @return número total de registros con reacciones adversas
     */
    public long countReaccionesAdversas() {
        String sql = "SELECT COUNT(*) FROM registro_vacunas WHERE reaccion_adversa = true";
        logQuery(sql);
        Object result = executeScalar(sql);

        long count = result instanceof Number ? ((Number) result).longValue() : 0;
        logger.debug("Conteo de registros con reacciones adversas: {}", count);

        return count;
    }

    /**
     * Actualiza información de reacción adversa
     * @param registroId ID del registro
     * @param tieneReaccion si tiene reacción adversa
     * @param descripcion descripción de la reacción
     * @return true si se actualizó correctamente
     */
    public boolean updateReaccionAdversa(Integer registroId, boolean tieneReaccion, String descripcion) {
        if (registroId == null) {
            return false;
        }

        try {
            String sql = "UPDATE registro_vacunas SET reaccion_adversa = ?, descripcion_reaccion = ? WHERE id = ?";
            logQuery(sql, registroId, tieneReaccion, descripcion);
            int rowsAffected = executeUpdate(sql, tieneReaccion, descripcion, registroId);

            boolean updated = rowsAffected > 0;
            logger.debug("Reacción adversa actualizada para registro {}: {}", registroId, updated);

            return updated;
        } catch (Exception e) {
            logger.error("Error al actualizar reacción adversa del registro " + registroId, e);
            return false;
        }
    }

    /**
     * Obtiene el historial completo de vacunación de un niño
     * @param ninoId ID del niño
     * @return historial de vacunación ordenado por fecha
     */
    public List<RegistroVacuna> getHistorialVacunacion(Integer ninoId) {
        return findByNino(ninoId);
    }

    /**
     * Verifica si un niño ya tiene una dosis específica de una vacuna
     * @param ninoId ID del niño
     * @param vacunaId ID de la vacuna
     * @param numeroDosis número de dosis
     * @return true si ya tiene la dosis
     */
    public boolean tieneDosis(Integer ninoId, Integer vacunaId, Integer numeroDosis) {
        if (ninoId == null || vacunaId == null || numeroDosis == null) {
            return false;
        }

        List<RegistroVacuna> registros = findByNinoAndVacuna(ninoId, vacunaId);

        return registros.stream()
                .anyMatch(r -> numeroDosis.equals(r.getNumeroDosis()));
    }

    /**
     * Obtiene el número de dosis aplicadas de una vacuna para un niño
     * @param ninoId ID del niño
     * @param vacunaId ID de la vacuna
     * @return número de dosis aplicadas
     */
    public int countDosisAplicadas(Integer ninoId, Integer vacunaId) {
        if (ninoId == null || vacunaId == null) {
            return 0;
        }

        List<RegistroVacuna> registros = findByNinoAndVacuna(ninoId, vacunaId);
        return registros.size();
    }

    /**
     * Obtiene estadísticas de registros de vacuna
     * @return String con estadísticas formateadas
     */
    public String getRegistroStatistics() {
        StringBuilder stats = new StringBuilder();

        try {
            long totalRegistros = count();
            long conReaccionesAdversas = countReaccionesAdversas();

            stats.append("ESTADÍSTICAS DE REGISTROS DE VACUNA\n");
            stats.append("===================================\n");
            stats.append("Total registros: ").append(totalRegistros).append("\n");
            stats.append("Con reacciones adversas: ").append(conReaccionesAdversas).append("\n");
            stats.append("Porcentaje reacciones adversas: ")
                    .append(totalRegistros > 0 ? String.format("%.2f%%", (conReaccionesAdversas * 100.0) / totalRegistros) : "0%")
                    .append("\n");

        } catch (Exception e) {
            logger.error("Error al obtener estadísticas de registros", e);
            stats.append("Error al generar estadísticas");
        }

        return stats.toString();
    }
}
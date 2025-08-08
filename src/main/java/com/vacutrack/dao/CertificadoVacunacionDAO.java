package com.vacutrack.dao;

import com.vacutrack.model.CertificadoVacunacion;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO para la entidad CertificadoVacunacion - VERSIÓN CORREGIDA
 * Maneja operaciones de base de datos para certificados de vacunación
 * Genera carnets digitales e impresos
 *
 * @author VACU-TRACK Team
 * @version 1.1 - Corregida
 */
public class CertificadoVacunacionDAO extends BaseDAO<CertificadoVacunacion, Integer> {

    // Instancia singleton
    private static CertificadoVacunacionDAO instance;

    // SQL Queries - CORREGIDAS según estructura real de BD
    private static final String INSERT_SQL =
            "INSERT INTO certificados_vacunacion (nino_id, codigo_certificado, " +
                    "fecha_generacion, porcentaje_completitud, url_archivo, vigente) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE certificados_vacunacion SET codigo_certificado = ?, " +
                    "fecha_generacion = ?, porcentaje_completitud = ?, url_archivo = ?, vigente = ? " +
                    "WHERE id = ?";

    private static final String FIND_BY_NINO_SQL =
            "SELECT * FROM certificados_vacunacion WHERE nino_id = ? AND vigente = true " +
                    "ORDER BY fecha_generacion DESC";

    private static final String FIND_BY_CODIGO_SQL =
            "SELECT * FROM certificados_vacunacion WHERE codigo_certificado = ? AND vigente = true";

    private static final String FIND_VIGENTES_SQL =
            "SELECT * FROM certificados_vacunacion WHERE vigente = true " +
                    "ORDER BY fecha_generacion DESC";

    private static final String FIND_BY_FECHA_RANGE_SQL =
            "SELECT * FROM certificados_vacunacion WHERE fecha_generacion BETWEEN ? AND ? " +
                    "AND vigente = true ORDER BY fecha_generacion DESC";

    private static final String FIND_RECENT_SQL =
            "SELECT * FROM certificados_vacunacion WHERE vigente = true " +
                    "ORDER BY fecha_generacion DESC LIMIT ?";

    private static final String UPDATE_VIGENCIA_SQL =
            "UPDATE certificados_vacunacion SET vigente = ? WHERE id = ?";

    private static final String UPDATE_PORCENTAJE_SQL =
            "UPDATE certificados_vacunacion SET porcentaje_completitud = ? WHERE id = ?";

    private static final String UPDATE_URL_ARCHIVO_SQL =
            "UPDATE certificados_vacunacion SET url_archivo = ? WHERE id = ?";

    /**
     * Constructor privado para patrón singleton
     */
    private CertificadoVacunacionDAO() {
        super();
    }

    /**
     * Obtiene la instancia singleton del DAO
     * @return instancia de CertificadoVacunacionDAO
     */
    public static synchronized CertificadoVacunacionDAO getInstance() {
        if (instance == null) {
            instance = new CertificadoVacunacionDAO();
        }
        return instance;
    }

    @Override
    protected String getTableName() {
        return "certificados_vacunacion";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected CertificadoVacunacion mapResultSetToEntity(ResultSet rs) throws SQLException {
        CertificadoVacunacion certificado = new CertificadoVacunacion();

        certificado.setId(getInteger(rs, "id"));
        certificado.setNinoId(getInteger(rs, "nino_id"));
        certificado.setCodigoCertificado(getString(rs, "codigo_certificado"));
        certificado.setFechaGeneracion(toLocalDateTime(rs.getTimestamp("fecha_generacion")));
        certificado.setPorcentajeCompletitud(getBigDecimal(rs, "porcentaje_completitud"));
        certificado.setUrlArchivo(getString(rs, "url_archivo"));
        certificado.setVigente(getBoolean(rs, "vigente"));

        return certificado;
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, CertificadoVacunacion certificado) throws SQLException {
        ps.setInt(1, certificado.getNinoId());
        ps.setString(2, certificado.getCodigoCertificado());
        ps.setTimestamp(3, certificado.getFechaGeneracion() != null ?
                java.sql.Timestamp.valueOf(certificado.getFechaGeneracion()) :
                java.sql.Timestamp.valueOf(LocalDateTime.now()));

        if (certificado.getPorcentajeCompletitud() != null) {
            ps.setBigDecimal(4, certificado.getPorcentajeCompletitud());
        } else {
            ps.setNull(4, Types.DECIMAL);
        }

        ps.setString(5, certificado.getUrlArchivo());
        ps.setBoolean(6, certificado.getVigente() != null ? certificado.getVigente() : true);
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, CertificadoVacunacion certificado) throws SQLException {
        ps.setString(1, certificado.getCodigoCertificado());
        ps.setTimestamp(2, certificado.getFechaGeneracion() != null ?
                java.sql.Timestamp.valueOf(certificado.getFechaGeneracion()) :
                java.sql.Timestamp.valueOf(LocalDateTime.now()));

        if (certificado.getPorcentajeCompletitud() != null) {
            ps.setBigDecimal(3, certificado.getPorcentajeCompletitud());
        } else {
            ps.setNull(3, Types.DECIMAL);
        }

        ps.setString(4, certificado.getUrlArchivo());
        ps.setBoolean(5, certificado.getVigente() != null ? certificado.getVigente() : true);
        ps.setInt(6, certificado.getId());
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
    protected void assignGeneratedId(CertificadoVacunacion certificado, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            certificado.setId(generatedKeys.getInt(1));
        }
    }

    // Métodos específicos para certificados de vacunación

    /**
     * Busca certificados por niño
     * @param ninoId ID del niño
     * @return lista de certificados del niño
     */
    public List<CertificadoVacunacion> findByNino(Integer ninoId) {
        if (ninoId == null) {
            return List.of();
        }

        logQuery(FIND_BY_NINO_SQL, ninoId);
        List<CertificadoVacunacion> certificados = executeQuery(FIND_BY_NINO_SQL, ninoId);

        logger.debug("Encontrados {} certificados para niño ID: {}", certificados.size(), ninoId);
        return certificados;
    }

    /**
     * Busca certificado por código
     * @param codigoCertificado código del certificado
     * @return Optional con el certificado encontrado
     */
    public Optional<CertificadoVacunacion> findByCodigo(String codigoCertificado) {
        if (codigoCertificado == null || codigoCertificado.trim().isEmpty()) {
            return Optional.empty();
        }

        logQuery(FIND_BY_CODIGO_SQL, codigoCertificado);
        CertificadoVacunacion certificado = executeQuerySingle(FIND_BY_CODIGO_SQL, codigoCertificado.trim());

        Optional<CertificadoVacunacion> result = Optional.ofNullable(certificado);
        logger.debug("Certificado encontrado por código {}: {}", codigoCertificado, result.isPresent());

        return result;
    }

    /**
     * Obtiene todos los certificados vigentes
     * @return lista de certificados vigentes
     */
    public List<CertificadoVacunacion> findVigentes() {
        logQuery(FIND_VIGENTES_SQL);
        List<CertificadoVacunacion> certificados = executeQuery(FIND_VIGENTES_SQL);

        logger.debug("Encontrados {} certificados vigentes", certificados.size());
        return certificados;
    }

    /**
     * Busca certificados por rango de fechas
     * @param fechaInicio fecha de inicio
     * @param fechaFin fecha de fin
     * @return lista de certificados en el rango de fechas
     */
    public List<CertificadoVacunacion> findByFechaRange(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return List.of();
        }

        logQuery(FIND_BY_FECHA_RANGE_SQL, fechaInicio, fechaFin);
        List<CertificadoVacunacion> certificados = executeQuery(FIND_BY_FECHA_RANGE_SQL,
                java.sql.Timestamp.valueOf(fechaInicio), java.sql.Timestamp.valueOf(fechaFin));

        logger.debug("Encontrados {} certificados entre {} y {}", certificados.size(), fechaInicio, fechaFin);
        return certificados;
    }

    /**
     * Busca certificados recientes
     * @param limite número máximo de certificados
     * @return lista de certificados más recientes
     */
    public List<CertificadoVacunacion> findRecent(int limite) {
        if (limite <= 0) {
            limite = 10; // Límite por defecto
        }

        logQuery(FIND_RECENT_SQL, limite);
        List<CertificadoVacunacion> certificados = executeQuery(FIND_RECENT_SQL, limite);

        logger.debug("Encontrados {} certificados recientes", certificados.size());
        return certificados;
    }

    /**
     * Actualiza la vigencia de un certificado
     * @param certificadoId ID del certificado
     * @param vigente nueva vigencia
     * @return true si se actualizó correctamente
     */
    public boolean updateVigencia(Integer certificadoId, boolean vigente) {
        if (certificadoId == null) {
            return false;
        }

        try {
            logQuery(UPDATE_VIGENCIA_SQL, vigente, certificadoId);
            int rowsAffected = executeUpdate(UPDATE_VIGENCIA_SQL, vigente, certificadoId);

            boolean updated = rowsAffected > 0;
            logger.debug("Vigencia actualizada para certificado {}: {}", certificadoId, updated);

            return updated;
        } catch (Exception e) {
            logger.error("Error al actualizar vigencia del certificado " + certificadoId, e);
            return false;
        }
    }

    /**
     * Actualiza el porcentaje de completitud de un certificado
     * @param certificadoId ID del certificado
     * @param porcentaje nuevo porcentaje
     * @return true si se actualizó correctamente
     */
    public boolean updatePorcentajeCompletitud(Integer certificadoId, java.math.BigDecimal porcentaje) {
        if (certificadoId == null || porcentaje == null) {
            return false;
        }

        try {
            logQuery(UPDATE_PORCENTAJE_SQL, porcentaje, certificadoId);
            int rowsAffected = executeUpdate(UPDATE_PORCENTAJE_SQL, porcentaje, certificadoId);

            boolean updated = rowsAffected > 0;
            logger.debug("Porcentaje actualizado para certificado {}: {}", certificadoId, updated);

            return updated;
        } catch (Exception e) {
            logger.error("Error al actualizar porcentaje del certificado " + certificadoId, e);
            return false;
        }
    }

    /**
     * Actualiza la URL del archivo de un certificado
     * @param certificadoId ID del certificado
     * @param urlArchivo nueva URL del archivo
     * @return true si se actualizó correctamente
     */
    public boolean updateUrlArchivo(Integer certificadoId, String urlArchivo) {
        if (certificadoId == null) {
            return false;
        }

        try {
            logQuery(UPDATE_URL_ARCHIVO_SQL, urlArchivo, certificadoId);
            int rowsAffected = executeUpdate(UPDATE_URL_ARCHIVO_SQL, urlArchivo, certificadoId);

            boolean updated = rowsAffected > 0;
            logger.debug("URL archivo actualizada para certificado {}: {}", certificadoId, updated);

            return updated;
        } catch (Exception e) {
            logger.error("Error al actualizar URL archivo del certificado " + certificadoId, e);
            return false;
        }
    }

    /**
     * SIMPLIFICADO: Genera un código único de certificado
     * @return código de certificado generado
     */
    public String generateCodigoCertificado() {
        try {
            // Formato simple: CERT-YYYY-NNNNNN
            int year = java.time.Year.now().getValue();
            long timestamp = System.currentTimeMillis();
            int sequence = (int) (timestamp % 1000000); // Últimos 6 dígitos del timestamp

            String codigo = String.format("CERT-%d-%06d", year, sequence);
            logger.debug("Código de certificado generado: {}", codigo);

            return codigo;
        } catch (Exception e) {
            logger.error("Error al generar código de certificado", e);
            return generateFallbackCode();
        }
    }

    /**
     * Genera un código de certificado alternativo
     * @return código de certificado alternativo
     */
    private String generateFallbackCode() {
        return "CERT-" + java.time.Year.now().getValue() + "-" +
                String.format("%06d", (int)(Math.random() * 1000000));
    }

    /**
     * Verifica si existe un certificado con el código dado
     * @param codigoCertificado código del certificado
     * @return true si existe el certificado
     */
    public boolean existsByCodigo(String codigoCertificado) {
        return findByCodigo(codigoCertificado).isPresent();
    }

    /**
     * Obtiene el último certificado de un niño
     * @param ninoId ID del niño
     * @return Optional con el último certificado
     */
    public Optional<CertificadoVacunacion> getUltimoCertificado(Integer ninoId) {
        List<CertificadoVacunacion> certificados = findByNino(ninoId);
        return certificados.isEmpty() ? Optional.empty() : Optional.of(certificados.get(0));
    }

    /**
     * SIMPLIFICADO: Crea un nuevo certificado de vacunación
     * @param ninoId ID del niño
     * @param porcentajeCompletitud porcentaje de vacunas completadas
     * @return certificado creado o null si hubo error
     */
    public CertificadoVacunacion crearCertificado(Integer ninoId, java.math.BigDecimal porcentajeCompletitud) {
        if (ninoId == null) {
            return null;
        }

        try {
            CertificadoVacunacion certificado = new CertificadoVacunacion();
            certificado.setNinoId(ninoId);
            certificado.setCodigoCertificado(generateCodigoCertificado());
            certificado.setFechaGeneracion(LocalDateTime.now());
            certificado.setPorcentajeCompletitud(porcentajeCompletitud != null ? porcentajeCompletitud : java.math.BigDecimal.ZERO);
            certificado.setVigente(true);

            if (save(certificado)) {
                logger.info("Certificado creado: {} para niño ID: {}",
                        certificado.getCodigoCertificado(), ninoId);
                return certificado;
            }

        } catch (Exception e) {
            logger.error("Error al crear certificado para niño " + ninoId, e);
        }

        return null;
    }

    /**
     * Invalida un certificado (marca como no vigente)
     * @param certificadoId ID del certificado
     * @return true si se invalidó correctamente
     */
    public boolean invalidarCertificado(Integer certificadoId) {
        return updateVigencia(certificadoId, false);
    }

    /**
     * Reactiva un certificado (marca como vigente)
     * @param certificadoId ID del certificado
     * @return true si se reactivó correctamente
     */
    public boolean reactivarCertificado(Integer certificadoId) {
        return updateVigencia(certificadoId, true);
    }

    /**
     * Cuenta certificados vigentes
     * @return número de certificados vigentes
     */
    public long countVigentes() {
        String sql = "SELECT COUNT(*) FROM certificados_vacunacion WHERE vigente = true";
        Object result = executeScalar(sql);
        return result instanceof Number ? ((Number) result).longValue() : 0;
    }

    /**
     * Cuenta certificados no vigentes
     * @return número de certificados no vigentes
     */
    public long countNoVigentes() {
        String sql = "SELECT COUNT(*) FROM certificados_vacunacion WHERE vigente = false";
        Object result = executeScalar(sql);
        return result instanceof Number ? ((Number) result).longValue() : 0;
    }

    /**
     * Busca certificados por porcentaje de completitud mínimo
     * @param porcentajeMinimo porcentaje mínimo
     * @return lista de certificados con porcentaje mayor o igual al especificado
     */
    public List<CertificadoVacunacion> findByPorcentajeMinimo(java.math.BigDecimal porcentajeMinimo) {
        if (porcentajeMinimo == null) {
            return List.of();
        }

        String sql = "SELECT * FROM certificados_vacunacion " +
                "WHERE vigente = true AND porcentaje_completitud >= ? " +
                "ORDER BY porcentaje_completitud DESC";

        logQuery(sql, porcentajeMinimo);
        List<CertificadoVacunacion> certificados = executeQuery(sql, porcentajeMinimo);

        logger.debug("Encontrados {} certificados con porcentaje >= {}",
                certificados.size(), porcentajeMinimo);
        return certificados;
    }

    /**
     * Obtiene certificados completos (100% completitud)
     * @return lista de certificados completos
     */
    public List<CertificadoVacunacion> findCertificadosCompletos() {
        return findByPorcentajeMinimo(new java.math.BigDecimal("100.00"));
    }

    /**
     * CORREGIDO: Obtiene estadísticas de certificados (solo con campos reales)
     * @return String con estadísticas formateadas
     */
    public String getCertificadoStatistics() {
        StringBuilder stats = new StringBuilder();

        try {
            long totalCertificados = count();
            long vigentes = countVigentes();
            long noVigentes = countNoVigentes();

            stats.append("ESTADÍSTICAS DE CERTIFICADOS\n");
            stats.append("============================\n");
            stats.append("Total certificados: ").append(totalCertificados).append("\n");
            stats.append("Vigentes: ").append(vigentes).append("\n");
            stats.append("No vigentes: ").append(noVigentes).append("\n");

            // Estadísticas por completitud
            long completos = findCertificadosCompletos().size();
            long parciales = vigentes - completos;

            stats.append("\nPOR COMPLETITUD\n");
            stats.append("===============\n");
            stats.append("Certificados completos (100%): ").append(completos).append("\n");
            stats.append("Certificados parciales (<100%): ").append(parciales).append("\n");

        } catch (Exception e) {
            logger.error("Error al obtener estadísticas de certificados", e);
            stats.append("Error al generar estadísticas: ").append(e.getMessage());
        }

        return stats.toString();
    }

    /**
     * Elimina físicamente certificados no vigentes antiguos
     * @param diasAntiguedad días de antigüedad para considerar eliminación
     * @return número de certificados eliminados
     */
    public int cleanupCertificadosAntiguos(int diasAntiguedad) {
        if (diasAntiguedad <= 0) {
            diasAntiguedad = 365; // Un año por defecto
        }

        String sql = "DELETE FROM certificados_vacunacion " +
                "WHERE vigente = false AND fecha_generacion < DATE_SUB(CURDATE(), INTERVAL ? DAY)";

        try {
            int deleted = executeUpdate(sql, diasAntiguedad);
            logger.info("Eliminados {} certificados antiguos no vigentes", deleted);
            return deleted;
        } catch (Exception e) {
            logger.error("Error al limpiar certificados antiguos", e);
            return 0;
        }
    }
}
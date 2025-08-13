package com.vacutrack.dao;

import com.vacutrack.model.EsquemaVacunacion;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DAO para la entidad EsquemaVacunacion - VERSIÓN CORREGIDA
 * Maneja operaciones de base de datos para esquemas de vacunación del MSP Ecuador
 * Incluye el esquema nacional completo con todas las edades y dosis
 *
 * @author VACU-TRACK Team
 * @version 1.1 - Corregida
 */
public class EsquemaVacunacionDAO extends BaseDAO<EsquemaVacunacion, Integer> {

    // Instancia singleton
    private static EsquemaVacunacionDAO instance;

    // SQL Queries - CORREGIDAS según estructura real de BD
    private static final String INSERT_SQL =
            "INSERT INTO esquema_vacunacion (vacuna_id, numero_dosis, edad_aplicacion_dias, " +
                    "edad_minima_dias, edad_maxima_dias, descripcion_edad, es_refuerzo, " +
                    "intervalo_dosis_anterior, observaciones, activo) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE esquema_vacunacion SET vacuna_id = ?, numero_dosis = ?, edad_aplicacion_dias = ?, " +
                    "edad_minima_dias = ?, edad_maxima_dias = ?, descripcion_edad = ?, es_refuerzo = ?, " +
                    "intervalo_dosis_anterior = ?, observaciones = ?, activo = ? WHERE id = ?";

    private static final String FIND_BY_VACUNA_SQL =
            "SELECT e.*, v.nombre as vacuna_nombre, v.descripcion as vacuna_descripcion, " +
                    "v.codigo as vacuna_codigo " +
                    "FROM esquema_vacunacion e " +
                    "LEFT JOIN vacunas v ON e.vacuna_id = v.id " +
                    "WHERE e.vacuna_id = ? AND e.activo = true " +
                    "ORDER BY e.numero_dosis";

    private static final String FIND_BY_EDAD_SQL =
            "SELECT e.*, v.nombre as vacuna_nombre, v.descripcion as vacuna_descripcion, " +
                    "v.codigo as vacuna_codigo " +
                    "FROM esquema_vacunacion e " +
                    "LEFT JOIN vacunas v ON e.vacuna_id = v.id " +
                    "WHERE e.edad_aplicacion_dias <= ? AND e.activo = true " +
                    "ORDER BY e.edad_aplicacion_dias, v.nombre";

    private static final String FIND_BY_EDAD_RANGE_SQL =
            "SELECT e.*, v.nombre as vacuna_nombre, v.descripcion as vacuna_descripcion, " +
                    "v.codigo as vacuna_codigo " +
                    "FROM esquema_vacunacion e " +
                    "LEFT JOIN vacunas v ON e.vacuna_id = v.id " +
                    "WHERE e.edad_aplicacion_dias BETWEEN ? AND ? AND e.activo = true " +
                    "ORDER BY e.edad_aplicacion_dias, v.nombre";

    private static final String FIND_REFUERZOS_SQL =
            "SELECT e.*, v.nombre as vacuna_nombre, v.descripcion as vacuna_descripcion, " +
                    "v.codigo as vacuna_codigo " +
                    "FROM esquema_vacunacion e " +
                    "LEFT JOIN vacunas v ON e.vacuna_id = v.id " +
                    "WHERE e.es_refuerzo = true AND e.activo = true " +
                    "ORDER BY e.edad_aplicacion_dias, v.nombre";

    private static final String FIND_ESQUEMA_COMPLETO_SQL =
            "SELECT e.*, v.nombre as vacuna_nombre, v.descripcion as vacuna_descripcion, " +
                    "v.codigo as vacuna_codigo " +
                    "FROM esquema_vacunacion e " +
                    "LEFT JOIN vacunas v ON e.vacuna_id = v.id " +
                    "WHERE e.activo = true " +
                    "ORDER BY e.edad_aplicacion_dias, v.nombre, e.numero_dosis";

    private static final String FIND_FOR_AGE_SQL =
            "SELECT e.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo " +
                    "FROM esquema_vacunacion e " +
                    "LEFT JOIN vacunas v ON e.vacuna_id = v.id " +
                    "WHERE e.edad_aplicacion_dias = ? AND e.activo = true " +
                    "ORDER BY v.nombre, e.numero_dosis";

    private static final String COUNT_BY_VACUNA_SQL =
            "SELECT COUNT(*) FROM esquema_vacunacion WHERE vacuna_id = ? AND activo = true";

    // Edades estándar del esquema ecuatoriano (en días)
    public static final int EDAD_NACIMIENTO = 0;
    public static final int EDAD_2_MESES = 60;
    public static final int EDAD_4_MESES = 120;
    public static final int EDAD_6_MESES = 180;
    public static final int EDAD_12_MESES = 365;
    public static final int EDAD_15_MESES = 450;
    public static final int EDAD_18_MESES = 540;
    public static final int EDAD_4_AÑOS = 1460;
    public static final int EDAD_5_AÑOS = 1825;
    public static final int EDAD_9_AÑOS = 3285;

    /**
     * Constructor privado para patrón singleton
     */
    private EsquemaVacunacionDAO() {
        super();
    }

    /**
     * Obtiene la instancia singleton del DAO
     * @return instancia de EsquemaVacunacionDAO
     */
    public static synchronized EsquemaVacunacionDAO getInstance() {
        if (instance == null) {
            instance = new EsquemaVacunacionDAO();
        }
        return instance;
    }

    @Override
    protected String getTableName() {
        return "esquema_vacunacion";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected EsquemaVacunacion mapResultSetToEntity(ResultSet rs) throws SQLException {
        EsquemaVacunacion esquema = new EsquemaVacunacion();

        esquema.setId(getInteger(rs, "id"));
        esquema.setVacunaId(getInteger(rs, "vacuna_id"));
        esquema.setNumeroDosis(getInteger(rs, "numero_dosis"));
        esquema.setEdadAplicacionDias(getInteger(rs, "edad_aplicacion_dias"));
        esquema.setEdadMinimaDias(getInteger(rs, "edad_minima_dias"));
        esquema.setEdadMaximaDias(getInteger(rs, "edad_maxima_dias"));
        esquema.setDescripcionEdad(getString(rs, "descripcion_edad"));
        esquema.setEsRefuerzo(getBoolean(rs, "es_refuerzo"));
        esquema.setIntervaloDosisAnterior(getInteger(rs, "intervalo_dosis_anterior"));
        esquema.setObservaciones(getString(rs, "observaciones"));
        esquema.setActivo(getBoolean(rs, "activo"));

        // Mapear información de vacuna si está disponible
        try {
            String vacunaNombre = getString(rs, "vacuna_nombre");
            if (vacunaNombre != null) {
                esquema.setVacunaNombre(vacunaNombre);
                esquema.setVacunaDescripcion(getString(rs, "vacuna_descripcion"));
                esquema.setVacunaCodigo(getString(rs, "vacuna_codigo"));
            }
        } catch (SQLException e) {
            // Información de vacuna no disponible, ignorar
            logger.debug("Información de vacuna no disponible en el ResultSet");
        }

        return esquema;
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, EsquemaVacunacion esquema) throws SQLException {
        ps.setInt(1, esquema.getVacunaId());
        ps.setInt(2, esquema.getNumeroDosis() != null ? esquema.getNumeroDosis() : 1);
        ps.setInt(3, esquema.getEdadAplicacionDias());

        if (esquema.getEdadMinimaDias() != null) {
            ps.setInt(4, esquema.getEdadMinimaDias());
        } else {
            ps.setNull(4, Types.INTEGER);
        }

        if (esquema.getEdadMaximaDias() != null) {
            ps.setInt(5, esquema.getEdadMaximaDias());
        } else {
            ps.setNull(5, Types.INTEGER);
        }

        ps.setString(6, esquema.getDescripcionEdad());
        ps.setBoolean(7, esquema.getEsRefuerzo() != null ? esquema.getEsRefuerzo() : false);

        if (esquema.getIntervaloDosisAnterior() != null) {
            ps.setInt(8, esquema.getIntervaloDosisAnterior());
        } else {
            ps.setNull(8, Types.INTEGER);
        }

        ps.setString(9, esquema.getObservaciones());
        ps.setBoolean(10, esquema.getActivo() != null ? esquema.getActivo() : true);
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, EsquemaVacunacion esquema) throws SQLException {
        ps.setInt(1, esquema.getVacunaId());
        ps.setInt(2, esquema.getNumeroDosis() != null ? esquema.getNumeroDosis() : 1);
        ps.setInt(3, esquema.getEdadAplicacionDias());

        if (esquema.getEdadMinimaDias() != null) {
            ps.setInt(4, esquema.getEdadMinimaDias());
        } else {
            ps.setNull(4, Types.INTEGER);
        }

        if (esquema.getEdadMaximaDias() != null) {
            ps.setInt(5, esquema.getEdadMaximaDias());
        } else {
            ps.setNull(5, Types.INTEGER);
        }

        ps.setString(6, esquema.getDescripcionEdad());
        ps.setBoolean(7, esquema.getEsRefuerzo() != null ? esquema.getEsRefuerzo() : false);

        if (esquema.getIntervaloDosisAnterior() != null) {
            ps.setInt(8, esquema.getIntervaloDosisAnterior());
        } else {
            ps.setNull(8, Types.INTEGER);
        }

        ps.setString(9, esquema.getObservaciones());
        ps.setBoolean(10, esquema.getActivo() != null ? esquema.getActivo() : true);
        ps.setInt(11, esquema.getId());
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
    protected void assignGeneratedId(EsquemaVacunacion esquema, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            esquema.setId(generatedKeys.getInt(1));
        }
    }

    // Métodos específicos para esquemas de vacunación

    /**
     * Busca esquemas por vacuna
     * @param vacunaId ID de la vacuna
     * @return lista de esquemas para la vacuna
     */
    public List<EsquemaVacunacion> findByVacuna(Integer vacunaId) {
        if (vacunaId == null) {
            return List.of();
        }

        logQuery(FIND_BY_VACUNA_SQL, vacunaId);
        List<EsquemaVacunacion> esquemas = executeQuery(FIND_BY_VACUNA_SQL, vacunaId);

        logger.debug("Encontrados {} esquemas para vacuna ID: {}", esquemas.size(), vacunaId);
        return esquemas;
    }

    /**
     * Busca esquemas aplicables hasta cierta edad
     * @param edadDias edad en días
     * @return lista de esquemas aplicables
     */
    public List<EsquemaVacunacion> findByEdad(Integer edadDias) {
        if (edadDias == null || edadDias < 0) {
            return List.of();
        }

        logQuery(FIND_BY_EDAD_SQL, edadDias);
        List<EsquemaVacunacion> esquemas = executeQuery(FIND_BY_EDAD_SQL, edadDias);

        logger.debug("Encontrados {} esquemas para edad {} días", esquemas.size(), edadDias);
        return esquemas;
    }

    /**
     * Busca esquemas en rango de edad
     * @param edadMinimaDias edad mínima en días
     * @param edadMaximaDias edad máxima en días
     * @return lista de esquemas en el rango
     */
    public List<EsquemaVacunacion> findByEdadRange(Integer edadMinimaDias, Integer edadMaximaDias) {
        if (edadMinimaDias == null || edadMaximaDias == null ||
                edadMinimaDias < 0 || edadMaximaDias < edadMinimaDias) {
            return List.of();
        }

        logQuery(FIND_BY_EDAD_RANGE_SQL, edadMinimaDias, edadMaximaDias);
        List<EsquemaVacunacion> esquemas = executeQuery(FIND_BY_EDAD_RANGE_SQL, edadMinimaDias, edadMaximaDias);

        logger.debug("Encontrados {} esquemas entre {} y {} días", esquemas.size(), edadMinimaDias, edadMaximaDias);
        return esquemas;
    }

    /**
     * Busca esquemas de refuerzo
     * @return lista de esquemas de refuerzo
     */
    public List<EsquemaVacunacion> findRefuerzos() {
        logQuery(FIND_REFUERZOS_SQL);
        List<EsquemaVacunacion> esquemas = executeQuery(FIND_REFUERZOS_SQL);

        logger.debug("Encontrados {} esquemas de refuerzo", esquemas.size());
        return esquemas;
    }

    /**
     * Obtiene el esquema completo de vacunación
     * @return lista completa del esquema nacional
     */
    public List<EsquemaVacunacion> getEsquemaCompleto() {
        logQuery(FIND_ESQUEMA_COMPLETO_SQL);
        List<EsquemaVacunacion> esquemas = executeQuery(FIND_ESQUEMA_COMPLETO_SQL);

        logger.debug("Esquema completo: {} entradas", esquemas.size());
        return esquemas;
    }

    /**
     * Busca esquemas para edad específica
     * @param edadDias edad exacta en días
     * @return lista de esquemas para esa edad
     */
    public List<EsquemaVacunacion> findForAge(Integer edadDias) {
        if (edadDias == null || edadDias < 0) {
            return List.of();
        }

        logQuery(FIND_FOR_AGE_SQL, edadDias);
        List<EsquemaVacunacion> esquemas = executeQuery(FIND_FOR_AGE_SQL, edadDias);

        logger.debug("Encontrados {} esquemas para edad exacta {} días", esquemas.size(), edadDias);
        return esquemas;
    }

    /**
     * Cuenta esquemas por vacuna
     * @param vacunaId ID de la vacuna
     * @return número de esquemas para la vacuna
     */
    public long countByVacuna(Integer vacunaId) {
        if (vacunaId == null) {
            return 0;
        }

        logQuery(COUNT_BY_VACUNA_SQL, vacunaId);
        Object result = executeScalar(COUNT_BY_VACUNA_SQL, vacunaId);

        long count = result instanceof Number ? ((Number) result).longValue() : 0;
        logger.debug("Conteo de esquemas para vacuna ID {}: {}", vacunaId, count);

        return count;
    }

    /**
     * Cuenta esquemas de refuerzo
     * @return número de esquemas de refuerzo
     */
    public long countRefuerzos() {
        String sql = "SELECT COUNT(*) FROM esquema_vacunacion WHERE es_refuerzo = true AND activo = true";
        Object result = executeScalar(sql);
        return result instanceof Number ? ((Number) result).longValue() : 0;
    }

    /**
     * Obtiene vacunas pendientes para un niño según su edad
     * @param edadDias edad del niño en días
     * @param vacunasAplicadas lista de IDs de vacunas ya aplicadas
     * @return lista de esquemas pendientes
     */
    public List<EsquemaVacunacion> getVacunasPendientes(Integer edadDias, List<Integer> vacunasAplicadas) {
        if (edadDias == null || edadDias < 0) {
            return List.of();
        }

        List<EsquemaVacunacion> esquemas = findByEdad(edadDias);

        if (vacunasAplicadas != null && !vacunasAplicadas.isEmpty()) {
            esquemas = esquemas.stream()
                    .filter(e -> !vacunasAplicadas.contains(e.getVacunaId()))
                    .collect(Collectors.toList()); // ✅ compatible con Java 8+
        }

        logger.debug("Vacunas pendientes para edad {} días: {}", edadDias, esquemas.size());
        return esquemas;
    }

    /**
     * Obtiene próximas vacunas según edad
     * @param edadDias edad actual del niño
     * @param diasAdelante días hacia adelante para buscar
     * @return lista de próximas vacunas
     */
    public List<EsquemaVacunacion> getProximasVacunas(Integer edadDias, Integer diasAdelante) {
        if (edadDias == null || diasAdelante == null || edadDias < 0 || diasAdelante <= 0) {
            return List.of();
        }

        Integer edadMinima = edadDias + 1;
        Integer edadMaxima = edadDias + diasAdelante;

        return findByEdadRange(edadMinima, edadMaxima);
    }

    /**
     * SIMPLIFICADO: Inicializa el esquema básico de vacunación del Ecuador
     * @return número de esquemas creados
     */
    public int initializeEsquemaBasico() {
        int created = 0;

        try {
            // Verificar si ya existe esquema
            if (count() > 0) {
                logger.info("Ya existe esquema de vacunación en la base de datos");
                return 0;
            }

            logger.info("Inicializando esquema básico de vacunación del Ecuador...");

            // Crear esquemas básicos (solo con campos que existen en BD)
            EsquemaVacunacion[] esquemas = {
                    // Al nacer
                    createEsquemaBasico(1, 1, EDAD_NACIMIENTO, "Al nacer", false, "BCG - Tuberculosis"),
                    createEsquemaBasico(2, 1, EDAD_NACIMIENTO, "Al nacer", false, "Hepatitis B - Primera dosis"),

                    // 2 meses
                    createEsquemaBasico(3, 1, EDAD_2_MESES, "2 meses", false, "Rotavirus - Primera dosis"),
                    createEsquemaBasico(4, 1, EDAD_2_MESES, "2 meses", false, "Polio IPV - Primera dosis"),
                    createEsquemaBasico(5, 1, EDAD_2_MESES, "2 meses", false, "Neumococo - Primera dosis"),
                    createEsquemaBasico(6, 1, EDAD_2_MESES, "2 meses", false, "Pentavalente - Primera dosis"),
                    createEsquemaBasico(7, 1, EDAD_2_MESES, "2 meses", false, "Polio OPV - Primera dosis"),

                    // 4 meses
                    createEsquemaBasico(3, 2, EDAD_4_MESES, "4 meses", false, "Rotavirus - Segunda dosis"),
                    createEsquemaBasico(4, 2, EDAD_4_MESES, "4 meses", false, "Polio IPV - Segunda dosis"),
                    createEsquemaBasico(5, 2, EDAD_4_MESES, "4 meses", false, "Neumococo - Segunda dosis"),
                    createEsquemaBasico(6, 2, EDAD_4_MESES, "4 meses", false, "Pentavalente - Segunda dosis"),
                    createEsquemaBasico(7, 2, EDAD_4_MESES, "4 meses", false, "Polio OPV - Segunda dosis"),

                    // 6 meses
                    createEsquemaBasico(3, 3, EDAD_6_MESES, "6 meses", false, "Rotavirus - Tercera dosis"),
                    createEsquemaBasico(4, 3, EDAD_6_MESES, "6 meses", false, "Polio IPV - Tercera dosis"),
                    createEsquemaBasico(5, 3, EDAD_6_MESES, "6 meses", false, "Neumococo - Tercera dosis"),
                    createEsquemaBasico(6, 3, EDAD_6_MESES, "6 meses", false, "Pentavalente - Tercera dosis"),
                    createEsquemaBasico(7, 3, EDAD_6_MESES, "6 meses", false, "Polio OPV - Tercera dosis"),
                    createEsquemaBasico(8, 1, EDAD_6_MESES, "6 meses", false, "Influenza - Primera dosis"),
                    createEsquemaBasico(8, 2, EDAD_6_MESES + 30, "7 meses", false, "Influenza - Segunda dosis"),

                    // 12 meses
                    createEsquemaBasico(9, 1, EDAD_12_MESES, "12 meses", false, "SRP - Primera dosis"),
                    createEsquemaBasico(10, 1, EDAD_12_MESES, "12 meses", false, "Fiebre Amarilla - Primera dosis"),

                    // 18 meses
                    createEsquemaBasico(9, 2, EDAD_18_MESES, "18 meses", true, "SRP - Segunda dosis (refuerzo)"),
                    createEsquemaBasico(11, 1, EDAD_18_MESES, "18 meses", false, "Varicela - Primera dosis")
            };

            // Insertar esquemas
            for (EsquemaVacunacion esquema : esquemas) {
                if (save(esquema)) {
                    created++;
                    logger.debug("Esquema creado: Vacuna {} - {} días - Dosis {}",
                            esquema.getVacunaId(), esquema.getEdadAplicacionDias(), esquema.getNumeroDosis());
                }
            }

            logger.info("Inicialización del esquema básico completada. Creados: {}", created);

        } catch (Exception e) {
            logger.error("Error al inicializar esquema básico", e);
        }

        return created;
    }

    /**
     * CORREGIDO: Método auxiliar para crear un esquema básico
     */
    private EsquemaVacunacion createEsquemaBasico(Integer vacunaId, Integer numeroDosis, Integer edadDias,
                                                  String descripcion, Boolean esRefuerzo, String observaciones) {
        EsquemaVacunacion esquema = new EsquemaVacunacion();
        esquema.setVacunaId(vacunaId);
        esquema.setNumeroDosis(numeroDosis);
        esquema.setEdadAplicacionDias(edadDias);
        esquema.setDescripcionEdad(descripcion);
        esquema.setEsRefuerzo(esRefuerzo);
        esquema.setObservaciones(observaciones);
        esquema.setActivo(true);
        return esquema;
    }

    /**
     * Obtiene esquema por edad en formato legible
     * @param edadDias edad en días
     * @return String con el esquema formateado
     */
    public String getEsquemaFormateado(Integer edadDias) {
        List<EsquemaVacunacion> esquemas = findForAge(edadDias);

        if (esquemas.isEmpty()) {
            return "No hay vacunas programadas para esta edad";
        }

        StringBuilder formato = new StringBuilder();
        String edadTexto = convertirEdadATexto(edadDias);

        formato.append("VACUNAS PARA ").append(edadTexto.toUpperCase()).append("\n");
        formato.append("========================================").append("\n");

        for (EsquemaVacunacion esquema : esquemas) {
            formato.append("• ").append(esquema.getVacunaNombre() != null ?
                    esquema.getVacunaNombre() : "Vacuna ID " + esquema.getVacunaId());
            formato.append(" - Dosis ").append(esquema.getNumeroDosis());

            if (esquema.getEsRefuerzo() != null && esquema.getEsRefuerzo()) {
                formato.append(" (REFUERZO)");
            }

            formato.append("\n");

            if (esquema.getObservaciones() != null) {
                formato.append("  ").append(esquema.getObservaciones()).append("\n");
            }

            formato.append("\n");
        }

        return formato.toString();
    }

    /**
     * Convierte edad en días a texto legible
     * @param edadDias edad en días
     * @return String con edad legible
     */
    private String convertirEdadATexto(Integer edadDias) {
        if (edadDias == null || edadDias < 0) {
            return "Edad no válida";
        }

        if (edadDias == 0) {
            return "Al nacer";
        } else if (edadDias <= 31) {
            return edadDias + " días";
        } else if (edadDias <= 365) {
            int meses = edadDias / 30;
            return meses + " mes" + (meses > 1 ? "es" : "");
        } else {
            int años = edadDias / 365;
            int mesesRestantes = (edadDias % 365) / 30;

            String texto = años + " año" + (años > 1 ? "s" : "");
            if (mesesRestantes > 0) {
                texto += " y " + mesesRestantes + " mes" + (mesesRestantes > 1 ? "es" : "");
            }
            return texto;
        }
    }

    /**
     * Desactiva un esquema de vacunación
     * @param esquemaId ID del esquema
     * @return true si se desactivó correctamente
     */
    public boolean deactivateEsquema(Integer esquemaId) {
        if (esquemaId == null) {
            return false;
        }

        String sql = "UPDATE esquema_vacunacion SET activo = false WHERE id = ?";
        try {
            int rowsAffected = executeUpdate(sql, esquemaId);
            boolean deactivated = rowsAffected > 0;

            if (deactivated) {
                logger.info("Esquema desactivado ID: {}", esquemaId);
            }

            return deactivated;
        } catch (Exception e) {
            logger.error("Error al desactivar esquema ID: {}", esquemaId, e);
            return false;
        }
    }

    /**
     * Activa un esquema de vacunación
     * @param esquemaId ID del esquema
     * @return true si se activó correctamente
     */
    public boolean activateEsquema(Integer esquemaId) {
        if (esquemaId == null) {
            return false;
        }

        String sql = "UPDATE esquema_vacunacion SET activo = true WHERE id = ?";
        try {
            int rowsAffected = executeUpdate(sql, esquemaId);
            boolean activated = rowsAffected > 0;

            if (activated) {
                logger.info("Esquema activado ID: {}", esquemaId);
            }

            return activated;
        } catch (Exception e) {
            logger.error("Error al activar esquema ID: {}", esquemaId, e);
            return false;
        }
    }

    /**
     * CORREGIDO: Obtiene estadísticas del esquema de vacunación
     * @return String con estadísticas formateadas
     */
    public String getEsquemaStatistics() {
        StringBuilder stats = new StringBuilder();

        try {
            long totalEsquemas = count();
            long refuerzos = countRefuerzos();
            long noRefuerzos = totalEsquemas - refuerzos;

            stats.append("ESTADÍSTICAS DEL ESQUEMA DE VACUNACIÓN\n");
            stats.append("======================================\n");
            stats.append("Total entradas: ").append(totalEsquemas).append("\n");
            stats.append("Dosis regulares: ").append(noRefuerzos).append("\n");
            stats.append("Dosis de refuerzo: ").append(refuerzos).append("\n");

            stats.append("\nDISTRIBUCIÓN POR EDAD\n");
            stats.append("====================\n");
            stats.append("Al nacer: ").append(findForAge(EDAD_NACIMIENTO).size()).append(" vacunas\n");
            stats.append("2 meses: ").append(findForAge(EDAD_2_MESES).size()).append(" vacunas\n");
            stats.append("4 meses: ").append(findForAge(EDAD_4_MESES).size()).append(" vacunas\n");
            stats.append("6 meses: ").append(findForAge(EDAD_6_MESES).size()).append(" vacunas\n");
            stats.append("12 meses: ").append(findForAge(EDAD_12_MESES).size()).append(" vacunas\n");
            stats.append("18 meses: ").append(findForAge(EDAD_18_MESES).size()).append(" vacunas\n");
            stats.append("4 años: ").append(findForAge(EDAD_4_AÑOS).size()).append(" vacunas\n");

        } catch (Exception e) {
            logger.error("Error al obtener estadísticas del esquema", e);
            stats.append("Error al generar estadísticas: ").append(e.getMessage());
        }

        return stats.toString();
    }

    /**
     * Busca esquemas activos
     * @return lista de esquemas activos
     */
    public List<EsquemaVacunacion> findActive() {
        String sql = "SELECT e.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo " +
                "FROM esquema_vacunacion e " +
                "LEFT JOIN vacunas v ON e.vacuna_id = v.id " +
                "WHERE e.activo = true " +
                "ORDER BY e.edad_aplicacion_dias, v.nombre, e.numero_dosis";

        logQuery(sql);
        List<EsquemaVacunacion> esquemas = executeQuery(sql);

        logger.debug("Encontrados {} esquemas activos", esquemas.size());
        return esquemas;
    }

    /**
     * Busca esquemas inactivos
     * @return lista de esquemas inactivos
     */
    public List<EsquemaVacunacion> findInactive() {
        String sql = "SELECT e.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo " +
                "FROM esquema_vacunacion e " +
                "LEFT JOIN vacunas v ON e.vacuna_id = v.id " +
                "WHERE e.activo = false " +
                "ORDER BY e.edad_aplicacion_dias, v.nombre, e.numero_dosis";

        logQuery(sql);
        List<EsquemaVacunacion> esquemas = executeQuery(sql);

        logger.debug("Encontrados {} esquemas inactivos", esquemas.size());
        return esquemas;
    }

    /**
     * Busca esquemas por descripción de edad
     * @param descripcionEdad descripción a buscar
     * @return lista de esquemas que coinciden
     */
    public List<EsquemaVacunacion> findByDescripcionEdad(String descripcionEdad) {
        if (descripcionEdad == null || descripcionEdad.trim().isEmpty()) {
            return List.of();
        }

        String sql = "SELECT e.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo " +
                "FROM esquema_vacunacion e " +
                "LEFT JOIN vacunas v ON e.vacuna_id = v.id " +
                "WHERE UPPER(e.descripcion_edad) LIKE UPPER(?) AND e.activo = true " +
                "ORDER BY e.edad_aplicacion_dias, v.nombre";

        String searchPattern = "%" + descripcionEdad.trim() + "%";
        logQuery(sql, searchPattern);
        List<EsquemaVacunacion> esquemas = executeQuery(sql, searchPattern);

        logger.debug("Encontrados {} esquemas con descripción '{}'", esquemas.size(), descripcionEdad);
        return esquemas;
    }

    /**
     * Obtiene las próximas 5 vacunas para un niño según su edad
     * @param edadActualDias edad actual del niño en días
     * @return lista de las próximas 5 vacunas programadas
     */
    public List<EsquemaVacunacion> getProximas5Vacunas(Integer edadActualDias) {
        if (edadActualDias == null || edadActualDias < 0) {
            return List.of();
        }

        String sql = "SELECT e.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo " +
                "FROM esquema_vacunacion e " +
                "LEFT JOIN vacunas v ON e.vacuna_id = v.id " +
                "WHERE e.edad_aplicacion_dias >= ? AND e.activo = true " +
                "ORDER BY e.edad_aplicacion_dias, v.nombre " +
                "LIMIT 5";

        logQuery(sql, edadActualDias);
        List<EsquemaVacunacion> esquemas = executeQuery(sql, edadActualDias);

        logger.debug("Próximas {} vacunas para edad {} días", esquemas.size(), edadActualDias);
        return esquemas;
    }

    /**
     * Verifica si existe un esquema específico
     * @param vacunaId ID de la vacuna
     * @param numeroDosis número de dosis
     * @return true si existe el esquema
     */
    public boolean existsEsquema(Integer vacunaId, Integer numeroDosis) {
        if (vacunaId == null || numeroDosis == null) {
            return false;
        }

        String sql = "SELECT 1 FROM esquema_vacunacion " +
                "WHERE vacuna_id = ? AND numero_dosis = ? AND activo = true " +
                "LIMIT 1";

        Object result = executeScalar(sql, vacunaId, numeroDosis);
        return result != null;
    }

    /**
     * Obtiene el esquema más temprano (primera vacuna)
     * @return Optional con el primer esquema por edad
     */
    public Optional<EsquemaVacunacion> getPrimerEsquema() {
        String sql = "SELECT e.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo " +
                "FROM esquema_vacunacion e " +
                "LEFT JOIN vacunas v ON e.vacuna_id = v.id " +
                "WHERE e.activo = true " +
                "ORDER BY e.edad_aplicacion_dias, e.numero_dosis " +
                "LIMIT 1";

        logQuery(sql);
        EsquemaVacunacion esquema = executeQuerySingle(sql);

        Optional<EsquemaVacunacion> result = Optional.ofNullable(esquema);
        logger.debug("Primer esquema encontrado: {}", result.isPresent());

        return result;
    }

    /**
     * Obtiene el esquema más tardío (última vacuna del cronograma)
     * @return Optional con el último esquema por edad
     */
    public Optional<EsquemaVacunacion> getUltimoEsquema() {
        String sql = "SELECT e.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo " +
                "FROM esquema_vacunacion e " +
                "LEFT JOIN vacunas v ON e.vacuna_id = v.id " +
                "WHERE e.activo = true " +
                "ORDER BY e.edad_aplicacion_dias DESC, e.numero_dosis DESC " +
                "LIMIT 1";

        logQuery(sql);
        EsquemaVacunacion esquema = executeQuerySingle(sql);

        Optional<EsquemaVacunacion> result = Optional.ofNullable(esquema);
        logger.debug("Último esquema encontrado: {}", result.isPresent());

        return result;
    }

    /**
     * Cuenta esquemas por edad específica
     * @param edadDias edad en días
     * @return número de esquemas para esa edad
     */
    public long countByEdad(Integer edadDias) {
        if (edadDias == null) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM esquema_vacunacion " +
                "WHERE edad_aplicacion_dias = ? AND activo = true";

        Object result = executeScalar(sql, edadDias);
        long count = result instanceof Number ? ((Number) result).longValue() : 0;

        logger.debug("Conteo de esquemas para edad {} días: {}", edadDias, count);
        return count;
    }

    /**
     * Busca todas las dosis de una vacuna específica
     * @param vacunaId ID de la vacuna
     * @return lista ordenada de todas las dosis de la vacuna
     */
    public List<EsquemaVacunacion> getAllDosisForVacuna(Integer vacunaId) {
        if (vacunaId == null) {
            return List.of();
        }

        String sql = "SELECT e.*, v.nombre as vacuna_nombre, v.codigo as vacuna_codigo " +
                "FROM esquema_vacunacion e " +
                "LEFT JOIN vacunas v ON e.vacuna_id = v.id " +
                "WHERE e.vacuna_id = ? AND e.activo = true " +
                "ORDER BY e.numero_dosis, e.edad_aplicacion_dias";

        logQuery(sql, vacunaId);
        List<EsquemaVacunacion> esquemas = executeQuery(sql, vacunaId);

        logger.debug("Todas las dosis para vacuna ID {}: {}", vacunaId, esquemas.size());
        return esquemas;
    }

    /**
     * Obtiene resumen del esquema por edades principales
     * @return String con resumen formateado por edades clave
     */
    public String getResumenPorEdades() {
        StringBuilder resumen = new StringBuilder();

        resumen.append("RESUMEN ESQUEMA NACIONAL DE VACUNACIÓN\n");
        resumen.append("======================================\n\n");

        // Edades clave del esquema
        Integer[] edadesClave = {
                EDAD_NACIMIENTO, EDAD_2_MESES, EDAD_4_MESES, EDAD_6_MESES,
                EDAD_12_MESES, EDAD_18_MESES, EDAD_4_AÑOS, EDAD_5_AÑOS
        };

        for (Integer edad : edadesClave) {
            List<EsquemaVacunacion> esquemas = findForAge(edad);
            if (!esquemas.isEmpty()) {
                resumen.append(convertirEdadATexto(edad).toUpperCase()).append(":\n");
                for (EsquemaVacunacion esquema : esquemas) {
                    resumen.append("  • ").append(
                            esquema.getVacunaNombre() != null ?
                                    esquema.getVacunaNombre() :
                                    "Vacuna ID " + esquema.getVacunaId()
                    ).append(" (Dosis ").append(esquema.getNumeroDosis()).append(")");

                    if (esquema.getEsRefuerzo() != null && esquema.getEsRefuerzo()) {
                        resumen.append(" - REFUERZO");
                    }
                    resumen.append("\n");
                }
                resumen.append("\n");
            }
        }

        return resumen.toString();
    }

    /**
     * Limpia esquemas inactivos antiguos
     * @return número de esquemas eliminados
     */
    public int cleanupEsquemasInactivos() {
        String sql = "DELETE FROM esquema_vacunacion WHERE activo = false";

        try {
            int deleted = executeUpdate(sql);
            logger.info("Eliminados {} esquemas inactivos", deleted);
            return deleted;
        } catch (Exception e) {
            logger.error("Error al limpiar esquemas inactivos", e);
            return 0;
        }
    }
}

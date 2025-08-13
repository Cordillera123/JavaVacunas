package com.vacutrack.dao;

import com.vacutrack.model.CentroSalud;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO para la entidad CentroSalud - VERSIÓN CORREGIDA
 * Maneja operaciones de base de datos para centros de salud
 * Incluye datos de centros ficticios de Quito para el mapa
 *
 * @author VACU-TRACK Team
 * @version 1.1 - Corregida
 */
public class CentroSaludDAO extends BaseDAO<CentroSalud, Integer> {

    // Instancia singleton
    private static CentroSaludDAO instance;

    // SQL Queries
    private static final String INSERT_SQL =
            "INSERT INTO centros_salud (nombre, direccion, telefono, " +
                    "coordenada_x, coordenada_y, sector, activo, fecha_creacion) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE centros_salud SET nombre = ?, direccion = ?, telefono = ?, " +
                    "coordenada_x = ?, coordenada_y = ?, sector = ?, activo = ? WHERE id = ?";

    private static final String FIND_BY_NOMBRE_SQL =
            "SELECT * FROM centros_salud WHERE UPPER(nombre) LIKE UPPER(?) AND activo = true ORDER BY nombre";

    private static final String FIND_BY_SECTOR_SQL =
            "SELECT * FROM centros_salud WHERE sector = ? AND activo = true ORDER BY nombre";

    private static final String FIND_ACTIVE_SQL =
            "SELECT * FROM centros_salud WHERE activo = true ORDER BY nombre";

    private static final String FIND_BY_UBICACION_SQL =
            "SELECT * FROM centros_salud WHERE activo = true AND " +
                    "coordenada_x BETWEEN ? AND ? AND coordenada_y BETWEEN ? AND ? " +
                    "ORDER BY nombre";

    private static final String SEARCH_BY_DIRECCION_SQL =
            "SELECT * FROM centros_salud WHERE UPPER(direccion) LIKE UPPER(?) AND activo = true ORDER BY nombre";

    private static final String COUNT_BY_SECTOR_SQL =
            "SELECT COUNT(*) FROM centros_salud WHERE sector = ? AND activo = true";

    // Constantes para sectores (corregidas según la BD)
    public static final String SECTOR_CENTRO_HISTORICO = "Centro Histórico";
    public static final String SECTOR_NORTE = "Norte";
    public static final String SECTOR_SUR = "Sur";
    public static final String SECTOR_VALLES = "Valles";

    /**
     * Constructor privado para patrón singleton
     */
    private CentroSaludDAO() {
        super();
    }

    /**
     * Obtiene la instancia singleton del DAO
     * @return instancia de CentroSaludDAO
     */
    public static synchronized CentroSaludDAO getInstance() {
        if (instance == null) {
            instance = new CentroSaludDAO();
        }
        return instance;
    }

    @Override
    protected String getTableName() {
        return "centros_salud";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected CentroSalud mapResultSetToEntity(ResultSet rs) throws SQLException {
        CentroSalud centro = new CentroSalud();

        centro.setId(getInteger(rs, "id"));
        centro.setNombre(getString(rs, "nombre"));
        centro.setDireccion(getString(rs, "direccion"));
        centro.setTelefono(getString(rs, "telefono"));
        centro.setCoordenadaX(getBigDecimal(rs, "coordenada_x"));
        centro.setCoordenadaY(getBigDecimal(rs, "coordenada_y"));
        centro.setSector(getString(rs, "sector"));
        centro.setActivo(getBoolean(rs, "activo"));
        centro.setFechaCreacion(toLocalDateTime(rs.getTimestamp("fecha_creacion")));

        return centro;
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, CentroSalud centro) throws SQLException {
        ps.setString(1, centro.getNombre());
        ps.setString(2, centro.getDireccion());
        ps.setString(3, centro.getTelefono());

        // Manejar coordenadas nulas
        if (centro.getCoordenadaX() != null) {
            ps.setBigDecimal(4, centro.getCoordenadaX());
        } else {
            ps.setNull(4, Types.DECIMAL);
        }

        if (centro.getCoordenadaY() != null) {
            ps.setBigDecimal(5, centro.getCoordenadaY());
        } else {
            ps.setNull(5, Types.DECIMAL);
        }

        ps.setString(6, centro.getSector() != null ? centro.getSector() : SECTOR_CENTRO_HISTORICO);
        ps.setBoolean(7, centro.getActivo() != null ? centro.getActivo() : true);
        ps.setTimestamp(8, java.sql.Timestamp.valueOf(
                centro.getFechaCreacion() != null ? centro.getFechaCreacion() : LocalDateTime.now()));
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, CentroSalud centro) throws SQLException {
        ps.setString(1, centro.getNombre());
        ps.setString(2, centro.getDireccion());
        ps.setString(3, centro.getTelefono());

        // Manejar coordenadas nulas
        if (centro.getCoordenadaX() != null) {
            ps.setBigDecimal(4, centro.getCoordenadaX());
        } else {
            ps.setNull(4, Types.DECIMAL);
        }

        if (centro.getCoordenadaY() != null) {
            ps.setBigDecimal(5, centro.getCoordenadaY());
        } else {
            ps.setNull(5, Types.DECIMAL);
        }

        ps.setString(6, centro.getSector() != null ? centro.getSector() : SECTOR_CENTRO_HISTORICO);
        ps.setBoolean(7, centro.getActivo() != null ? centro.getActivo() : true);
        ps.setInt(8, centro.getId());
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
    protected void assignGeneratedId(CentroSalud centro, ResultSet generatedKeys) throws SQLException {
        if (generatedKeys.next()) {
            centro.setId(generatedKeys.getInt(1));
        }
    }

    // Métodos específicos para centros de salud

    /**
     * Busca centros por nombre (búsqueda parcial)
     * @param nombre nombre o parte del nombre del centro
     * @return lista de centros que coinciden con la búsqueda
     */
    public List<CentroSalud> findByNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return List.of();
        }

        String searchPattern = "%" + nombre.trim() + "%";
        logQuery(FIND_BY_NOMBRE_SQL, searchPattern);
        List<CentroSalud> centros = executeQuery(FIND_BY_NOMBRE_SQL, searchPattern);

        logger.debug("Encontrados {} centros con nombre '{}'", centros.size(), nombre);
        return centros;
    }

    /**
     * Busca centros por sector
     * @param sector sector del centro de salud
     * @return lista de centros del sector especificado
     */
    public List<CentroSalud> findBySector(String sector) {
        if (sector == null || sector.trim().isEmpty()) {
            return List.of();
        }

        logQuery(FIND_BY_SECTOR_SQL, sector);
        List<CentroSalud> centros = executeQuery(FIND_BY_SECTOR_SQL, sector.trim());

        logger.debug("Encontrados {} centros del sector '{}'", centros.size(), sector);
        return centros;
    }

    /**
     * Obtiene todos los centros activos
     * @return lista de centros activos
     */
    public List<CentroSalud> findActive() {
        logQuery(FIND_ACTIVE_SQL);
        List<CentroSalud> centros = executeQuery(FIND_ACTIVE_SQL);

        logger.debug("Encontrados {} centros activos", centros.size());
        return centros;
    }

    /**
     * Busca centros por ubicación geográfica (usando coordenadas)
     * @param xMin coordenada X mínima
     * @param xMax coordenada X máxima
     * @param yMin coordenada Y mínima
     * @param yMax coordenada Y máxima
     * @return lista de centros en el área especificada
     */
    public List<CentroSalud> findByUbicacion(double xMin, double xMax,
                                             double yMin, double yMax) {
        logQuery(FIND_BY_UBICACION_SQL, xMin, xMax, yMin, yMax);
        List<CentroSalud> centros = executeQuery(FIND_BY_UBICACION_SQL,
                xMin, xMax, yMin, yMax);

        logger.debug("Encontrados {} centros en área geográfica especificada", centros.size());
        return centros;
    }

    /**
     * Busca centros por dirección (búsqueda parcial)
     * @param direccion dirección o parte de la dirección
     * @return lista de centros que coinciden con la búsqueda
     */
    public List<CentroSalud> searchByDireccion(String direccion) {
        if (direccion == null || direccion.trim().isEmpty()) {
            return List.of();
        }

        String searchPattern = "%" + direccion.trim() + "%";
        logQuery(SEARCH_BY_DIRECCION_SQL, searchPattern);
        List<CentroSalud> centros = executeQuery(SEARCH_BY_DIRECCION_SQL, searchPattern);

        logger.debug("Encontrados {} centros con dirección '{}'", centros.size(), direccion);
        return centros;
    }

    /**
     * Cuenta centros por sector
     * @param sector sector del centro
     * @return número de centros del sector especificado
     */
    public long countBySector(String sector) {
        if (sector == null || sector.trim().isEmpty()) {
            return 0;
        }

        logQuery(COUNT_BY_SECTOR_SQL, sector);
        Object result = executeScalar(COUNT_BY_SECTOR_SQL, sector.trim());

        long count = result instanceof Number ? ((Number) result).longValue() : 0;
        logger.debug("Conteo de centros del sector '{}': {}", sector, count);

        return count;
    }

    /**
     * Obtiene centros cercanos a una ubicación específica
     * @param latitud latitud de referencia
     * @param longitud longitud de referencia
     * @param radioKm radio en kilómetros
     * @return lista de centros dentro del radio especificado
     */
    public List<CentroSalud> findCercanos(double latitud, double longitud, double radioKm) {
        // Aproximación simple para el área de búsqueda
        double deltaLat = radioKm / 111.0; // Aproximadamente 1 grado = 111 km
        double deltaLon = radioKm / (111.0 * Math.cos(Math.toRadians(latitud)));

        double latMin = latitud - deltaLat;
        double latMax = latitud + deltaLat;
        double lonMin = longitud - deltaLon;
        double lonMax = longitud + deltaLon;

        List<CentroSalud> centros = findByUbicacion(latMin, latMax, lonMin, lonMax);

        logger.debug("Encontrados {} centros cercanos a [{}, {}] en radio de {} km",
                centros.size(), latitud, longitud, radioKm);

        return centros;
    }

    /**
     * Obtiene centros de Quito (datos ficticios para el mapa)
     * @return lista de centros de salud de Quito
     */
    public List<CentroSalud> getCentrosQuito() {
        // Coordenadas aproximadas de Quito
        double quitoLatMin = -0.35;
        double quitoLatMax = -0.05;
        double quitoLonMin = -78.6;
        double quitoLonMax = -78.4;

        return findByUbicacion(quitoLatMin, quitoLatMax, quitoLonMin, quitoLonMax);
    }

    /**
     * CORREGIDO: Inicializa centros ficticios de Quito si no existen (solo campos de BD)
     * @return número de centros creados
     */
    public int initializeCentrosQuito() {
        int created = 0;

        try {
            // Verificar si ya existen centros
            if (count() > 0) {
                logger.info("Ya existen centros en la base de datos");
                return 0;
            }

            // Crear centros ficticios de Quito - SOLO con campos que existen en la BD
            CentroSalud[] centrosFicticios = {
                    createCentroBasico("Centro de Salud No 1 Centro Histórico",
                            "García Moreno N4-36 y Espejo",
                            "02-295-1234",
                            -0.22010490, -78.51234500),

                    createCentroBasico("Subcentro de Salud La Merced",
                            "Cuenca 740 y Chile",
                            "02-295-5678",
                            -0.21850760, -78.51456230),

                    createCentroBasico("Centro de Salud San Marcos",
                            "Junín E1-54 y Montúfar",
                            "02-295-9012",
                            -0.21567890, -78.50982340),

                    createCentroBasico("Dispensario Central",
                            "Venezuela N3-67 y Olmedo",
                            "02-295-3456",
                            -0.22345670, -78.51678900),

                    createCentroBasico("Centro de Salud Tipo A San Sebastián",
                            "San Sebastián y Pte. Luis Cordero",
                            "02-295-7890",
                            -0.21789010, -78.52345670)
            };

            // Insertar centros ficticios
            for (CentroSalud centro : centrosFicticios) {
                if (save(centro)) {
                    created++;
                    logger.info("Centro de salud creado: {}", centro.getNombre());
                }
            }

            logger.info("Inicialización de centros de Quito completada. Creados: {}", created);

        } catch (Exception e) {
            logger.error("Error al inicializar centros de Quito", e);
        }

        return created;
    }

    /**
     * CORREGIDO: Método auxiliar para crear un centro de salud básico
     */
    private CentroSalud createCentroBasico(String nombre, String direccion, String telefono,
                                           double latitud, double longitud) {
        CentroSalud centro = new CentroSalud();
        centro.setNombre(nombre);
        centro.setDireccion(direccion);
        centro.setTelefono(telefono);
        centro.setCoordenadaX(new java.math.BigDecimal(latitud));
        centro.setCoordenadaY(new java.math.BigDecimal(longitud));
        centro.setSector(SECTOR_CENTRO_HISTORICO);
        centro.setActivo(true);
        centro.setFechaCreacion(LocalDateTime.now());
        return centro;
    }

    /**
     * Desactiva un centro de salud (soft delete)
     * @param centroId ID del centro a desactivar
     * @return true si se desactivó correctamente
     */
    public boolean deactivateCentro(Integer centroId) {
        if (centroId == null) {
            return false;
        }

        String sql = "UPDATE centros_salud SET activo = false WHERE id = ?";

        try {
            int affectedRows = executeUpdate(sql, centroId);
            boolean deactivated = affectedRows > 0;

            if (deactivated) {
                logger.info("Centro desactivado ID: {}", centroId);
            } else {
                logger.warn("No se pudo desactivar el centro ID: {}", centroId);
            }

            return deactivated;

        } catch (Exception e) {
            logger.error("Error al desactivar centro ID: {}", centroId, e);
            return false;
        }
    }

    /**
     * Activa un centro de salud
     * @param centroId ID del centro a activar
     * @return true si se activó correctamente
     */
    public boolean activateCentro(Integer centroId) {
        if (centroId == null) {
            return false;
        }

        String sql = "UPDATE centros_salud SET activo = true WHERE id = ?";

        try {
            int affectedRows = executeUpdate(sql, centroId);
            boolean activated = affectedRows > 0;

            if (activated) {
                logger.info("Centro activado ID: {}", centroId);
            } else {
                logger.warn("No se pudo activar el centro ID: {}", centroId);
            }

            return activated;

        } catch (Exception e) {
            logger.error("Error al activar centro ID: {}", centroId, e);
            return false;
        }
    }

    /**
     * CORREGIDO: Obtiene estadísticas de centros de salud (solo con campos que existen)
     * @return String con estadísticas formateadas
     */
    public String getCentroStatistics() {
        StringBuilder stats = new StringBuilder();

        try {
            long totalCentros = count();
            long activeCentros = findActive().size();
            long inactiveCentros = totalCentros - activeCentros;

            stats.append("ESTADÍSTICAS DE CENTROS DE SALUD\n");
            stats.append("================================\n");
            stats.append("Total centros: ").append(totalCentros).append("\n");
            stats.append("Centros activos: ").append(activeCentros).append("\n");
            stats.append("Centros inactivos: ").append(inactiveCentros).append("\n");

            stats.append("\nPOR SECTOR\n");
            stats.append("==========\n");
            stats.append("Centro Histórico: ").append(countBySector(SECTOR_CENTRO_HISTORICO)).append("\n");
            stats.append("Norte: ").append(countBySector(SECTOR_NORTE)).append("\n");
            stats.append("Sur: ").append(countBySector(SECTOR_SUR)).append("\n");
            stats.append("Valles: ").append(countBySector(SECTOR_VALLES)).append("\n");

        } catch (Exception e) {
            logger.error("Error al obtener estadísticas de centros", e);
            stats.append("Error al generar estadísticas: ").append(e.getMessage());
        }

        return stats.toString();
    }

    /**
     * Busca centros que contengan un término en nombre o dirección
     * @param termino término de búsqueda
     * @return lista de centros que coinciden
     */
    public List<CentroSalud> searchCentros(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            return findActive();
        }

        String sql = "SELECT * FROM centros_salud WHERE activo = true " +
                "AND (UPPER(nombre) LIKE UPPER(?) OR UPPER(direccion) LIKE UPPER(?)) " +
                "ORDER BY nombre";

        String searchPattern = "%" + termino.trim() + "%";
        logQuery(sql, searchPattern, searchPattern);
        List<CentroSalud> centros = executeQuery(sql, searchPattern, searchPattern);

        logger.debug("Encontrados {} centros con término '{}'", centros.size(), termino);
        return centros;
    }

    /**
     * Verifica si un centro con el mismo nombre ya existe
     * @param nombre nombre del centro
     * @return true si ya existe
     */
    public boolean existsByNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT 1 FROM centros_salud WHERE UPPER(nombre) = UPPER(?) AND activo = true LIMIT 1";
        Object result = executeScalar(sql, nombre.trim());
        return result != null;
    }

    /**
     * Verifica si un centro con el mismo nombre ya existe excluyendo uno específico
     * @param nombre nombre del centro
     * @param excludeId ID del centro a excluir
     * @return true si ya existe en otro centro
     */
    public boolean existsByNombreExcluding(String nombre, Integer excludeId) {
        if (nombre == null || nombre.trim().isEmpty() || excludeId == null) {
            return false;
        }

        String sql = "SELECT 1 FROM centros_salud WHERE UPPER(nombre) = UPPER(?) AND id != ? AND activo = true LIMIT 1";
        Object result = executeScalar(sql, nombre.trim(), excludeId);
        return result != null;
    }

    /**
     * Busca centros por municipio (usando el campo sector como aproximación)
     * @param municipio municipio del centro de salud
     * @return lista de centros del municipio/sector especificado
     */
    public List<CentroSalud> findByMunicipio(String municipio) {
        if (municipio == null || municipio.trim().isEmpty()) {
            return List.of();
        }

        // En el modelo actual, usamos el campo 'sector' como municipio
        // para simplificar y mantener compatibilidad
        return findBySector(municipio);
    }
}
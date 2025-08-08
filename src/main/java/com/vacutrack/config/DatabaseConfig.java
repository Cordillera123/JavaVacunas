package com.vacutrack.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Configuración de base de datos para VACU-TRACK
 * Maneja la conexión usando HikariCP como pool de conexiones
 * Soporta configuración por archivo de properties
 *
 * @author VACU-TRACK Team
 * @version 1.0
 */
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    // Instancia singleton
    private static DatabaseConfig instance;
    private static HikariDataSource dataSource;

    // Configuración por defecto (desarrollo)
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/vacu_track_db?useSSL=false&serverTimezone=America/Guayaquil&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";

    // Configuración del pool
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 20;
    private static final int DEFAULT_MINIMUM_IDLE = 5;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30000; // 30 segundos
    private static final long DEFAULT_IDLE_TIMEOUT = 600000; // 10 minutos
    private static final long DEFAULT_MAX_LIFETIME = 1800000; // 30 minutos

    // Archivo de configuración
    private static final String CONFIG_FILE = "/database.properties";

    // Constructor privado para singleton
    private DatabaseConfig() {
        inicializarDataSource();
    }

    /**
     * Obtiene la instancia singleton de DatabaseConfig
     * @return instancia de DatabaseConfig
     */
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    /**
     * Inicializa el DataSource con HikariCP
     */
    private void inicializarDataSource() {
        try {
            Properties props = cargarConfiguracion();

            HikariConfig config = new HikariConfig();

            // Configuración de conexión
            config.setJdbcUrl(props.getProperty("database.url", DEFAULT_URL));
            config.setUsername(props.getProperty("database.username", DEFAULT_USERNAME));
            config.setPassword(props.getProperty("database.password", DEFAULT_PASSWORD));
            config.setDriverClassName(props.getProperty("database.driver", DEFAULT_DRIVER));

            // Configuración del pool
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("database.pool.maxSize",
                    String.valueOf(DEFAULT_MAXIMUM_POOL_SIZE))));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("database.pool.minIdle",
                    String.valueOf(DEFAULT_MINIMUM_IDLE))));
            config.setConnectionTimeout(Long.parseLong(props.getProperty("database.pool.connectionTimeout",
                    String.valueOf(DEFAULT_CONNECTION_TIMEOUT))));
            config.setIdleTimeout(Long.parseLong(props.getProperty("database.pool.idleTimeout",
                    String.valueOf(DEFAULT_IDLE_TIMEOUT))));
            config.setMaxLifetime(Long.parseLong(props.getProperty("database.pool.maxLifetime",
                    String.valueOf(DEFAULT_MAX_LIFETIME))));

            // Configuraciones adicionales
            config.setPoolName("VacuTrackPool");
            config.setAutoCommit(true);
            config.setConnectionTestQuery("SELECT 1");
            config.setLeakDetectionThreshold(60000); // 1 minuto

            // Propiedades adicionales de MySQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            dataSource = new HikariDataSource(config);

            logger.info("DataSource inicializado correctamente con HikariCP");
            logger.info("URL: {}", props.getProperty("database.url", DEFAULT_URL));
            logger.info("Pool máximo: {}", config.getMaximumPoolSize());

            // Probar conexión
            probarConexion();

        } catch (Exception e) {
            logger.error("Error al inicializar DataSource", e);
            throw new RuntimeException("No se pudo inicializar la conexión a la base de datos", e);
        }
    }

    /**
     * Carga la configuración desde el archivo properties
     * @return Properties con la configuración
     */
    private Properties cargarConfiguracion() {
        Properties props = new Properties();

        try (InputStream input = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                props.load(input);
                logger.info("Configuración cargada desde {}", CONFIG_FILE);
            } else {
                logger.warn("Archivo de configuración {} no encontrado, usando valores por defecto", CONFIG_FILE);
                cargarConfiguracionPorDefecto(props);
            }
        } catch (IOException e) {
            logger.warn("Error al cargar configuración desde archivo, usando valores por defecto", e);
            cargarConfiguracionPorDefecto(props);
        }

        return props;
    }

    /**
     * Carga configuración por defecto
     * @param props Properties a llenar
     */
    private void cargarConfiguracionPorDefecto(Properties props) {
        props.setProperty("database.url", DEFAULT_URL);
        props.setProperty("database.username", DEFAULT_USERNAME);
        props.setProperty("database.password", DEFAULT_PASSWORD);
        props.setProperty("database.driver", DEFAULT_DRIVER);
        props.setProperty("database.pool.maxSize", String.valueOf(DEFAULT_MAXIMUM_POOL_SIZE));
        props.setProperty("database.pool.minIdle", String.valueOf(DEFAULT_MINIMUM_IDLE));
        props.setProperty("database.pool.connectionTimeout", String.valueOf(DEFAULT_CONNECTION_TIMEOUT));
        props.setProperty("database.pool.idleTimeout", String.valueOf(DEFAULT_IDLE_TIMEOUT));
        props.setProperty("database.pool.maxLifetime", String.valueOf(DEFAULT_MAX_LIFETIME));
    }

    /**
     * Prueba la conexión a la base de datos
     * @throws SQLException si hay error de conexión
     */
    private void probarConexion() throws SQLException {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                logger.info("Conexión a base de datos probada exitosamente");
            } else {
                throw new SQLException("No se pudo establecer conexión con la base de datos");
            }
        }
    }

    /**
     * Obtiene una conexión de la pool
     * @return Connection activa
     * @throws SQLException si hay error al obtener conexión
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource no inicializado");
        }

        Connection connection = dataSource.getConnection();
        if (connection == null) {
            throw new SQLException("No se pudo obtener conexión del pool");
        }

        return connection;
    }

    /**
     * Obtiene el DataSource
     * @return DataSource configurado
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Obtiene estadísticas del pool de conexiones
     * @return String con estadísticas
     */
    public String obtenerEstadisticasPool() {
        if (dataSource != null) {
            StringBuilder stats = new StringBuilder();
            stats.append("Pool Statistics:\n");
            stats.append("- Active Connections: ").append(dataSource.getHikariPoolMXBean().getActiveConnections()).append("\n");
            stats.append("- Idle Connections: ").append(dataSource.getHikariPoolMXBean().getIdleConnections()).append("\n");
            stats.append("- Total Connections: ").append(dataSource.getHikariPoolMXBean().getTotalConnections()).append("\n");
            stats.append("- Threads Awaiting: ").append(dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()).append("\n");
            return stats.toString();
        }
        return "DataSource no disponible";
    }

    /**
     * Verifica si el DataSource está activo
     * @return true si está activo
     */
    public boolean isDataSourceActive() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * Ejecuta una prueba de conectividad
     * @return true si la conexión es exitosa
     */
    public boolean probarConectividad() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(5);
        } catch (SQLException e) {
            logger.error("Error en prueba de conectividad", e);
            return false;
        }
    }

    /**
     * Cierra el DataSource y libera recursos
     */
    public synchronized void cerrarDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Cerrando DataSource...");
            dataSource.close();
            logger.info("DataSource cerrado correctamente");
        }
    }

    /**
     * Reinicia la conexión a la base de datos
     */
    public synchronized void reiniciarConexion() {
        logger.info("Reiniciando conexión a base de datos...");
        cerrarDataSource();
        dataSource = null;
        instance = null;
        getInstance(); // Reinicializa
    }

    // Métodos de utilidad para transacciones

    /**
     * Ejecuta una operación en una transacción
     * @param operacion operación a ejecutar
     * @throws SQLException si hay error en la transacción
     */
    public void ejecutarEnTransaccion(OperacionTransaccional operacion) throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            operacion.ejecutar(conn);

            conn.commit();
            logger.debug("Transacción completada exitosamente");

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.debug("Transacción revertida debido a error");
                } catch (SQLException rollbackEx) {
                    logger.error("Error al revertir transacción", rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    logger.error("Error al cerrar conexión", closeEx);
                }
            }
        }
    }

    /**
     * Interface funcional para operaciones transaccionales
     */
    @FunctionalInterface
    public interface OperacionTransaccional {
        void ejecutar(Connection connection) throws SQLException;
    }

    // Shutdown hook para cerrar conexiones al salir
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (instance != null) {
                instance.cerrarDataSource();
            }
        }));
    }

    // Métodos estáticos de conveniencia

    /**
     * Método estático para obtener una conexión rápidamente
     * @return Connection activa
     * @throws SQLException si hay error
     */
    public static Connection obtenerConexion() throws SQLException {
        return getInstance().getConnection();
    }

    /**
     * Método estático para verificar conectividad
     * @return true si hay conectividad
     */
    public static boolean verificarConectividad() {
        return getInstance().probarConectividad();
    }
}
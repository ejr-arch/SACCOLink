package saccolink.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SACCOLink JDBC connector for Oracle Database 19c.
 *
 * Builds a thin (Type 4) connection URL and hands out pooled connections
 * through {@link DriverManager}. Configuration is supplied once at startup
 * (see {@link #configure}) and can be tested without opening the GUI.
 */
public final class DBConnection {

    /** Cap how long a fresh connection waits for an unreachable host. */
    private static final int LOGIN_TIMEOUT_SECONDS = 15;

    static {
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
    }

    private static volatile String url;
    private static volatile String username;
    private static volatile String password;
    private static volatile boolean configured;

    private DBConnection() {
    }

    /**
     * Configure the connection against an Oracle service name (recommended).
     *
     * @param host     host name or IP of the database server
     * @param port     Oracle listener port (default 1521)
     * @param service  Oracle service name (e.g. XEPDB1, orclpdb1, ORCLCDB)
     * @param username database user (e.g. SACCOLINK)
     * @param password database password
     */
    public static void configure(String host, int port, String service,
                                 String username, String password) {
        url = "jdbc:oracle:thin:@//" + host + ":" + port + "/" + service;
        DBConnection.username = username;
        DBConnection.password = password;
        configured = true;
    }

    /**
     * Configure the connection using a raw JDBC URL (advanced / H2 demos).
     */
    public static void configureRaw(String jdbcUrl, String username, String password) {
        url = jdbcUrl;
        DBConnection.username = username;
        DBConnection.password = password;
        configured = true;
    }

    public static boolean isConfigured() {
        return configured && url != null;
    }

    /** Returns the configured JDBC URL (masked password never shown). */
    public static String getUrl() {
        return url;
    }

    public static String getUsername() {
        return username;
    }

    /** Opens a fresh connection to the configured database. */
    public static Connection getConnection() throws SQLException {
        if (!isConfigured()) {
            throw new SQLException("Database connection is not configured. " +
                    "Please open File > Connect and enter database details.");
        }
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "Oracle JDBC driver not found. Place ojdbc11.jar in src/lib/ and re-run build.sh.", e);
        }
        return DriverManager.getConnection(url, username, password);
    }

    /** Tests the configured connection and reports whether it succeeded. */
    public static void testConnection() throws SQLException {
        try (Connection c = getConnection()) {
            if (c.isClosed()) {
                throw new SQLException("Connection was closed unexpectedly.");
            }
        }
    }

    /** Closes any pooled resources held by the connector. */
    public static void shutdown() {
        configured = false;
        url = null;
        username = null;
        password = null;
    }
}

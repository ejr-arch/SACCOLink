package saccolink.db;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Small helper that centralises JDBC plumbing: silent close of the
 * {@link ResultSet} / {@link PreparedStatement} / {@link Connection}
 * trio inside try-with-resources.
 */
public final class Jdbc {

    private Jdbc() {
    }

    /** Closes a statement / result set without throwing. */
    public static void closeQuietly(AutoCloseable... closeables) {
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                    // best-effort cleanup
                }
            }
        }
    }

    /** Convenience: rollback a transaction if a connection is still open. */
    public static void rollbackQuietly(Connection c) {
        if (c != null) {
            try {
                c.rollback();
            } catch (SQLException ignored) {
                // no-op
            }
        }
    }

    /** Opens a Statement from the configured connection. */
    public static Statement statement() throws SQLException {
        return DBConnection.getConnection().createStatement();
    }

    /** Opens a PreparedStatement from the configured connection. */
    public static PreparedStatement prepare(String sql) throws SQLException {
        return DBConnection.getConnection().prepareStatement(sql);
    }

    /** Opens a CallableStatement from the configured connection. */
    public static CallableStatement prepareCall(String sql) throws SQLException {
        return DBConnection.getConnection().prepareCall(sql);
    }

    /**
     * Reads the current value of a sequence on the given connection. Used to
     * retrieve the PK assigned by a BEFORE INSERT trigger (classic
     * sequence + trigger pattern, no identity columns).
     */
    public static long currval(Connection c, String sequence) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + sequence + ".CURRVAL FROM DUAL");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
}

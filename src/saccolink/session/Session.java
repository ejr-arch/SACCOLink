package saccolink.session;

import saccolink.db.DBConnection;
import saccolink.model.AppUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Holds the currently logged-in application user (MEMBER or SACCO).
 *
 * <p>Every scoped database read attaches the logged-in member to the
 * {@code saccolink_ctx} application context on its own connection, so the
 * {@code V_MY_*} views only ever return that user's rows. SACCO attaches
 * {@code NULL} and therefore sees everything.</p>
 */
public final class Session {

    private static volatile AppUser user;

    private Session() {
    }

    public static void setUser(AppUser u) {
        user = u;
    }

    public static AppUser user() {
        return user;
    }

    public static void logout() {
        user = null;
    }

    public static boolean isLoggedIn() {
        return user != null;
    }

    public static boolean isSacco() {
        return user != null && user.isSacco();
    }

    public static boolean isMember() {
        return user != null && !user.isSacco();
    }

    /** Member id of the logged-in user, or null for SACCO. */
    public static Long memberId() {
        return user == null ? null : user.getMemberId();
    }

    /** Login name of the current user (used as REVIEWED_BY on approvals). */
    public static String username() {
        return user == null ? null : user.getUsername();
    }

    /**
     * Sets the application context on a connection so the V_MY_* views return
     * only the logged-in user's rows. Must be called on every fresh connection.
     */
    public static void attach(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "BEGIN PKG_APP_SESSION.SET_MEMBER(?); END;")) {
            Long mid = memberId();
            if (mid == null) {
                ps.setNull(1, Types.NUMERIC);
            } else {
                ps.setLong(1, mid);
            }
            ps.execute();
        }
    }

    /** Opens a new connection with the current user's context already set. */
    public static Connection openScopedConnection() throws SQLException {
        Connection c = DBConnection.getConnection();
        try {
            attach(c);
        } catch (SQLException e) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // best effort
            }
            throw e;
        }
        return c;
    }
}

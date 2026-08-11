package saccolink.service;

import saccolink.db.DBConnection;
import saccolink.model.AppUser;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Wraps SP_LOGIN (multi-user authentication against APP_USER) over JDBC.
 * Returns the authenticated user or null when the credentials are invalid.
 */
public final class UserService {

    private UserService() {
    }

    public static AppUser login(String username, String password) throws SQLException {
        String sql = "{call SP_LOGIN(?, ?, ?, ?, ?, ?, ?)}";
        try (Connection c = DBConnection.getConnection();
             CallableStatement cs = c.prepareCall(sql)) {
            cs.setString(1, username);
            cs.setString(2, password);
            cs.registerOutParameter(3, Types.NUMERIC);
            cs.registerOutParameter(4, Types.VARCHAR);
            cs.registerOutParameter(5, Types.NUMERIC);
            cs.registerOutParameter(6, Types.VARCHAR);
            cs.registerOutParameter(7, Types.NUMERIC);
            cs.execute();

            if (cs.getInt(7) == 0) {
                return null;
            }
            AppUser u = new AppUser();
            u.setUserId(cs.getLong(3));
            u.setUsername(username);
            u.setRole(cs.getString(4));
            u.setMemberId(cs.getObject(5) == null ? null : cs.getLong(5));
            u.setDisplayName(cs.getString(6));
            return u;
        }
    }
}

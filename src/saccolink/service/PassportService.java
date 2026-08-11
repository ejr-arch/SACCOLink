package saccolink.service;

import saccolink.db.DBConnection;
import saccolink.model.VerificationResult;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Wraps passport generation (SP_GENERATE_PASSPORT) and verification
 * (FN_VERIFY_PASSPORT) PL/SQL routines over JDBC.
 */
public final class PassportService {

    private PassportService() {
    }

    /**
     * Generates a 72-hour credit passport for a member.
     *
     * @return Object[]{passportId (long), qrToken (String)}
     * @throws SQLException if consent was not given (ORA-20002) or the member
     *                      has no current score
     */
    public static Object[] generate(long memberId) throws SQLException {
        String sql = "{call SP_GENERATE_PASSPORT(?, ?, ?)}";
        try (Connection c = DBConnection.getConnection();
             CallableStatement cs = c.prepareCall(sql)) {
            cs.setLong(1, memberId);
            cs.registerOutParameter(2, Types.NUMERIC);
            cs.registerOutParameter(3, Types.VARCHAR);
            cs.execute();
            long passportId = cs.getLong(2);
            String token = cs.getString(3);
            return new Object[]{passportId, token};
        } catch (SQLException e) {
            throw unwrap(e, memberId);
        }
    }

    /**
     * Verifies a QR token using FN_VERIFY_PASSPORT. Returns one of
     * VALID / EXPIRED / REVOKED / NOT_FOUND together with the passport detail.
     */
    public static VerificationResult verify(String token) throws SQLException {
        String sql = "{? = call FN_VERIFY_PASSPORT(?, ?, ?, ?, ?, ?)}";
        try (Connection c = DBConnection.getConnection();
             CallableStatement cs = c.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.setString(2, token);
            cs.registerOutParameter(3, Types.VARCHAR);   // member name
            cs.registerOutParameter(4, Types.NUMERIC);   // score value
            cs.registerOutParameter(5, Types.VARCHAR);   // score band
            cs.registerOutParameter(6, Types.TIMESTAMP); // generated at
            cs.registerOutParameter(7, Types.TIMESTAMP); // expires at
            cs.execute();

            String status = cs.getString(1);
            String name = cs.getString(3);
            Double score = cs.getObject(4) == null ? null : cs.getDouble(4);
            String band = cs.getString(5);
            Timestamp gen = cs.getTimestamp(6);
            Timestamp exp = cs.getTimestamp(7);
            return new VerificationResult(status, name, score, band, gen, exp);
        }
    }

    private static SQLException unwrap(SQLException e, long memberId) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("CONSENT_NOT_GIVEN") || msg.contains("ORA-20002")) {
            return new SQLException("Member " + memberId +
                    " has NOT given consent. Passports can only be generated with consent.", e);
        }
        if (msg.contains("MEMBER_NOT_FOUND") || msg.contains("ORA-20001")) {
            return new SQLException("Member " + memberId + " does not exist.", e);
        }
        if (msg.contains("ORA-01403") || msg.contains("no data found")) {
            return new SQLException("Member " + memberId + " has no current score. "
                    + "Compute a score first (Page 6).", e);
        }
        return e;
    }
}

package saccolink.dao;

import saccolink.db.DBConnection;
import saccolink.model.PassportDetail;
import saccolink.session.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Data access over the read-only view V_PASSPORT_DETAIL. */
public class PassportDAO {

    public static List<PassportDetail> findAll(String status, String tokenLike) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM V_MY_PASSPORTS WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND PASSPORT_STATUS = ?");
            params.add(status);
        }
        if (tokenLike != null && !tokenLike.isBlank()) {
            sql.append(" AND QR_TOKEN LIKE ?");
            params.add("%" + tokenLike + "%");
        }
        sql.append(" ORDER BY PASSPORT_ID DESC");

        List<PassportDetail> list = new ArrayList<>();
        try (Connection c = Session.openScopedConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public static void revoke(String qrToken) throws SQLException {
        String sql = "UPDATE CREDIT_PASSPORT SET PASSPORT_STATUS = 'REVOKED' "
                + "WHERE QR_TOKEN = ? AND PASSPORT_STATUS = 'ACTIVE'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, qrToken);
            ps.executeUpdate();
        }
    }

    /** Calls SP_EXPIRE_PASSPORTS and returns the number of passports expired. */
    public static int expirePassports() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE CREDIT_PASSPORT SET PASSPORT_STATUS = 'EXPIRED' "
                     + "WHERE PASSPORT_STATUS = 'ACTIVE' AND EXPIRES_AT < SYSTIMESTAMP")) {
            return ps.executeUpdate();
        }
    }

    private static PassportDetail map(ResultSet rs) throws SQLException {
        PassportDetail p = new PassportDetail();
        p.setPassportId(rs.getLong("PASSPORT_ID"));
        p.setQrToken(rs.getString("QR_TOKEN"));
        p.setPassportStatus(rs.getString("PASSPORT_STATUS"));
        p.setGeneratedAt(rs.getTimestamp("GENERATED_AT"));
        p.setExpiresAt(rs.getTimestamp("EXPIRES_AT"));
        p.setViewCount(rs.getLong("VIEW_COUNT"));
        p.setMemberId(rs.getLong("MEMBER_ID"));
        p.setMemberName(rs.getString("FULL_NAME"));
        p.setNin(rs.getString("NIN"));
        p.setDistrict(rs.getString("DISTRICT"));
        p.setScoreValue(rs.getDouble("SCORE_VALUE"));
        p.setScoreBand(rs.getString("SCORE_BAND"));
        return p;
    }
}

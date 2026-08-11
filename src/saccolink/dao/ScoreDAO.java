package saccolink.dao;

import saccolink.db.DBConnection;
import saccolink.model.CreditScore;
import saccolink.session.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Data access for the CREDIT_SCORE table. */
public class ScoreDAO {

    /** Current scores (IS_CURRENT = 'Y'); scoped to the logged-in member. */
    public static List<CreditScore> findCurrent() throws SQLException {
        String sql = "SELECT SCORE_ID, MEMBER_ID, MEMBER_NAME, SCORE_VALUE, SCORE_BAND, "
                + "REPAYMENT_SCORE, SAVINGS_SCORE, COMPUTED_AT, IS_CURRENT "
                + "FROM V_MY_SCORES WHERE IS_CURRENT = 'Y' ORDER BY SCORE_VALUE DESC";
        List<CreditScore> list = new ArrayList<>();
        try (Connection c = Session.openScopedConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    /** Latest score for a single member (any status). */
    public static CreditScore latestForMember(long memberId) throws SQLException {
        String sql = "SELECT SCORE_ID, MEMBER_ID, MEMBER_NAME, SCORE_VALUE, SCORE_BAND, "
                + "REPAYMENT_SCORE, SAVINGS_SCORE, COMPUTED_AT, IS_CURRENT "
                + "FROM V_MY_SCORES WHERE MEMBER_ID = ? "
                + "ORDER BY SCORE_ID DESC FETCH FIRST 1 ROW ONLY";
        try (Connection c = Session.openScopedConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private static CreditScore map(ResultSet rs) throws SQLException {
        CreditScore s = new CreditScore();
        s.setScoreId(rs.getLong("SCORE_ID"));
        s.setMemberId(rs.getLong("MEMBER_ID"));
        s.setMemberName(rs.getString("MEMBER_NAME"));
        s.setScoreValue(rs.getDouble("SCORE_VALUE"));
        s.setScoreBand(rs.getString("SCORE_BAND"));
        s.setRepaymentScore(nullableDouble(rs, "REPAYMENT_SCORE"));
        s.setSavingsScore(nullableDouble(rs, "SAVINGS_SCORE"));
        s.setComputedAt(rs.getTimestamp("COMPUTED_AT"));
        s.setCurrent("Y".equalsIgnoreCase(rs.getString("IS_CURRENT")));
        return s;
    }

    /** Reads a nullable NUMBER column as Double (Oracle returns BigDecimal). */
    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double v = rs.getDouble(column);
        return rs.wasNull() ? null : v;
    }
}

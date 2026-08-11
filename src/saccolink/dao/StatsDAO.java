package saccolink.dao;

import saccolink.model.DashboardStats;
import saccolink.session.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Aggregate figures for the Dashboard (scoped to the logged-in user). */
public final class StatsDAO {

    private StatsDAO() {
    }

    /** SACCO-wide totals (all members). */
    public static DashboardStats saccoTotals() throws SQLException {
        String sql = "SELECT "
                + "  (SELECT COUNT(*) FROM MEMBER) AS MEMBER_COUNT, "
                + "  (SELECT COUNT(*) FROM LOAN_RECORD) AS LOAN_COUNT, "
                + "  (SELECT NVL(SUM(LOAN_AMOUNT), 0) FROM LOAN_RECORD "
                + "    WHERE LOAN_STATUS = 'ACTIVE') AS OUTSTANDING, "
                + "  (SELECT NVL(SUM(AMOUNT_CONTRIBUTED), 0) FROM SAVINGS_RECORD) AS TOTAL_SAVINGS, "
                + "  (SELECT COUNT(*) FROM CREDIT_SCORE WHERE IS_CURRENT = 'Y') AS SCORED_MEMBERS, "
                + "  (SELECT NVL(AVG(SCORE_VALUE), 0) FROM CREDIT_SCORE "
                + "    WHERE IS_CURRENT = 'Y') AS AVG_SCORE, "
                + "  (SELECT COUNT(*) FROM LOAN_REQUEST WHERE STATUS = 'PENDING') AS PENDING "
                + "FROM DUAL";
        DashboardStats s = new DashboardStats();
        try (Connection c = Session.openScopedConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                s.setMemberCount(rs.getLong("MEMBER_COUNT"));
                s.setLoanCount(rs.getLong("LOAN_COUNT"));
                s.setTotalOutstanding(rs.getDouble("OUTSTANDING"));
                s.setTotalSavings(rs.getDouble("TOTAL_SAVINGS"));
                s.setScoredMembers(rs.getLong("SCORED_MEMBERS"));
                s.setAvgScore(rs.getDouble("AVG_SCORE"));
                s.setPendingRequests(rs.getLong("PENDING"));
            }
        }
        return s;
    }

    /** Totals for the logged-in MEMBER only (via the V_MY_* views). */
    public static DashboardStats memberTotals() throws SQLException {
        String sql = "SELECT "
                + "  (SELECT COUNT(*) FROM V_MY_LOANS) AS LOAN_COUNT, "
                + "  (SELECT NVL(SUM(LOAN_AMOUNT), 0) FROM V_MY_LOANS "
                + "    WHERE LOAN_STATUS = 'ACTIVE') AS OUTSTANDING, "
                + "  (SELECT NVL(SUM(AMOUNT_CONTRIBUTED), 0) FROM V_MY_SAVINGS) AS TOTAL_SAVINGS, "
                + "  (SELECT COUNT(*) FROM V_MY_PASSPORTS) AS PASSPORT_COUNT, "
                + "  (SELECT SCORE_VALUE FROM V_MY_SCORES WHERE IS_CURRENT = 'Y' "
                + "    FETCH FIRST 1 ROW ONLY) AS SCORE_VALUE, "
                + "  (SELECT SCORE_BAND FROM V_MY_SCORES WHERE IS_CURRENT = 'Y' "
                + "    FETCH FIRST 1 ROW ONLY) AS SCORE_BAND "
                + "FROM DUAL";
        DashboardStats s = new DashboardStats();
        try (Connection c = Session.openScopedConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                s.setLoanCount(rs.getLong("LOAN_COUNT"));
                s.setTotalOutstanding(rs.getDouble("OUTSTANDING"));
                s.setTotalSavings(rs.getDouble("TOTAL_SAVINGS"));
                s.setPassportCount(rs.getLong("PASSPORT_COUNT"));
                double score = rs.getDouble("SCORE_VALUE");
                s.setScoreValue(rs.wasNull() ? null : score);
                s.setScoreBand(rs.getString("SCORE_BAND"));
            }
        }
        return s;
    }
}

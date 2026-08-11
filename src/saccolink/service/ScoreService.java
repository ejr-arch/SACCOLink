package saccolink.service;

import saccolink.db.DBConnection;
import saccolink.model.CreditScore;
import saccolink.model.Member;
import saccolink.dao.MemberDAO;
import saccolink.dao.ScoreDAO;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps the PL/SQL scoring engine (SP_COMPUTE_SCORE) exposed over JDBC and
 * re-implements the 60/40 formula in Java as a pure client-side fallback.
 */
public final class ScoreService {

    private ScoreService() {
    }

    /**
     * Calls SP_COMPUTE_SCORE for a member. Raises a user-friendly
     * {@link IllegalArgumentException} when the stored procedure rejects the
     * member (e.g. MEMBER_NOT_FOUND).
     *
     * @return the freshly computed current score, or null if none readable
     */
    public static CreditScore compute(long memberId) throws SQLException {
        String sql = "{call SP_COMPUTE_SCORE(?)}";
        try (Connection c = DBConnection.getConnection();
             CallableStatement cs = c.prepareCall(sql)) {
            cs.setLong(1, memberId);
            cs.execute();
        } catch (SQLException e) {
            throw unwrap(e, memberId);
        }
        return ScoreDAO.latestForMember(memberId);
    }

    /** Re-implements SP_COMPUTE_SCORE in pure Java (no DB procedure needed). */
    public static CreditScore computeLocally(Member m,
                                             double repaymentScore,
                                             double savingsScore) {
        double composite = Math.round(
                (repaymentScore * 0.6 + savingsScore * 0.4) * 8.5 * 100.0) / 100.0;
        composite = Math.min(composite, 850);
        CreditScore s = new CreditScore();
        s.setMemberId(m.getMemberId());
        s.setMemberName(m.getFullName());
        s.setScoreValue(composite);
        s.setScoreBand(band(composite));
        s.setRepaymentScore(repaymentScore);
        s.setSavingsScore(savingsScore);
        return s;
    }

    /** Maps a 0-850 score to its band (same rules as FN_GET_SCORE_BAND). */
    public static String band(double score) {
        if (score >= 700) return "EXCELLENT";
        if (score >= 550) return "GOOD";
        if (score >= 400) return "FAIR";
        return "THIN";
    }

    private static SQLException unwrap(SQLException e, long memberId) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("MEMBER_NOT_FOUND") || msg.contains("ORA-20001")) {
            return new SQLException("Member " + memberId + " does not exist.", e);
        }
        if (msg.contains("ORA-01403") || msg.contains("no data found")) {
            return new SQLException("No score could be computed - add loan or savings history first.", e);
        }
        return e;
    }
}

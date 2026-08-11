package saccolink.dao;

import saccolink.db.DBConnection;
import saccolink.model.LoanRequest;
import saccolink.model.MemberSummary;
import saccolink.session.Session;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the LOAN_REQUEST workflow.
 *
 * <p>Reads run on a scoped connection (see {@link Session}), so a MEMBER
 * only ever sees their own requests while SACCO sees all of them.</p>
 */
public class LoanRequestDAO {

    private static final String COLS =
            "SELECT r.REQUEST_ID, r.MEMBER_ID, m.FULL_NAME AS MEMBER_NAME, "
            + "r.REQUESTED_AMOUNT, r.PURPOSE, r.REQUESTED_AT, r.STATUS, "
            + "r.REVIEWED_BY, r.REVIEWED_AT FROM LOAN_REQUEST r "
            + "JOIN MEMBER m ON m.MEMBER_ID = r.MEMBER_ID";

    public static List<LoanRequest> findAll(String status) throws SQLException {
        StringBuilder sql = new StringBuilder(COLS).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        Long self = Session.isMember() ? Session.memberId() : null;
        if (self != null) {
            sql.append(" AND r.MEMBER_ID = ?");
            params.add(self);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND r.STATUS = ?");
            params.add(status);
        }
        sql.append(" ORDER BY r.REQUEST_ID DESC");

        List<LoanRequest> list = new ArrayList<>();
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

    /** Submits a loan request via SP_REQUEST_LOAN (member action). */
    public static void submit(long memberId, double amount, String purpose) throws SQLException {
        String sql = "{call SP_REQUEST_LOAN(?, ?, ?)}";
        try (Connection c = DBConnection.getConnection();
             CallableStatement cs = c.prepareCall(sql)) {
            cs.setLong(1, memberId);
            cs.setDouble(2, amount);
            cs.setString(3, purpose);
            cs.execute();
        }
    }

    /** Approves or rejects a request via SP_REVIEW_LOAN (SACCO action). */
    public static void review(long requestId, String decision, String reviewedBy) throws SQLException {
        String sql = "{call SP_REVIEW_LOAN(?, ?, ?)}";
        try (Connection c = DBConnection.getConnection();
             CallableStatement cs = c.prepareCall(sql)) {
            cs.setLong(1, requestId);
            cs.setString(2, decision);
            cs.setString(3, reviewedBy);
            cs.execute();
        }
    }

    /**
     * Reads a member's summary (loans, total savings, current score + band)
     * via PKG_SACCOINK_REPORT.MEMBER_SUMMARY for the creditworthiness check.
     */
    public static MemberSummary creditCheck(long memberId) throws SQLException {
        String sql = "{call PKG_SACCOINK_REPORT.MEMBER_SUMMARY(?, ?, ?, ?, ?)}";
        try (Connection c = DBConnection.getConnection();
             CallableStatement cs = c.prepareCall(sql)) {
            cs.setLong(1, memberId);
            cs.registerOutParameter(2, Types.NUMERIC);
            cs.registerOutParameter(3, Types.NUMERIC);
            cs.registerOutParameter(4, Types.NUMERIC);
            cs.registerOutParameter(5, Types.VARCHAR);
            cs.execute();

            MemberSummary s = new MemberSummary();
            s.setMemberId(memberId);
            s.setLoanCount(cs.getObject(2) == null ? 0 : cs.getLong(2));
            s.setTotalSavings(cs.getObject(3) == null ? 0 : cs.getDouble(3));
            Object score = cs.getObject(4);
            s.setScore(score == null ? null : ((Number) score).doubleValue());
            s.setBand(cs.getString(5));
            return s;
        }
    }

    private static LoanRequest map(ResultSet rs) throws SQLException {
        LoanRequest r = new LoanRequest();
        r.setRequestId(rs.getLong("REQUEST_ID"));
        r.setMemberId(rs.getLong("MEMBER_ID"));
        r.setMemberName(rs.getString("MEMBER_NAME"));
        r.setRequestedAmount(rs.getDouble("REQUESTED_AMOUNT"));
        r.setPurpose(rs.getString("PURPOSE"));
        r.setRequestedAt(rs.getTimestamp("REQUESTED_AT"));
        r.setStatus(rs.getString("STATUS"));
        r.setReviewedBy(rs.getString("REVIEWED_BY"));
        r.setReviewedAt(rs.getTimestamp("REVIEWED_AT"));
        return r;
    }
}

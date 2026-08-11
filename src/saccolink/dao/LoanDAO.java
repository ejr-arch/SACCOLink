package saccolink.dao;

import saccolink.db.DBConnection;
import saccolink.db.Jdbc;
import saccolink.model.LoanRecord;
import saccolink.session.Session;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Data access for the LOAN_RECORD table. */
public class LoanDAO {

    public static List<LoanRecord> findAll(Long memberId, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT LOAN_ID, MEMBER_ID, MEMBER_NAME, LOAN_AMOUNT, "
                + "DISBURSEMENT_DATE, REPAYMENT_DATE, LOAN_STATUS "
                + "FROM V_MY_LOANS WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (memberId != null) {
            sql.append(" AND MEMBER_ID = ?");
            params.add(memberId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND LOAN_STATUS = ?");
            params.add(status);
        }
        sql.append(" ORDER BY LOAN_ID DESC");

        List<LoanRecord> list = new ArrayList<>();
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

    public static long insert(LoanRecord l) throws SQLException {
        String sql = "INSERT INTO LOAN_RECORD (MEMBER_ID, LOAN_AMOUNT, DISBURSEMENT_DATE, "
                + "REPAYMENT_DATE, LOAN_STATUS) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, l.getMemberId());
            ps.setDouble(2, l.getLoanAmount());
            ps.setDate(3, l.getDisbursementDate());
            ps.setDate(4, l.getRepaymentDate());
            ps.setString(5, l.getLoanStatus());
            ps.executeUpdate();
            return Jdbc.currval(c, "SEQ_LOAN");
        }
    }

    public static void update(LoanRecord l) throws SQLException {
        String sql = "UPDATE LOAN_RECORD SET MEMBER_ID = ?, LOAN_AMOUNT = ?, "
                + "DISBURSEMENT_DATE = ?, REPAYMENT_DATE = ?, LOAN_STATUS = ? WHERE LOAN_ID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, l.getMemberId());
            ps.setDouble(2, l.getLoanAmount());
            ps.setDate(3, l.getDisbursementDate());
            ps.setDate(4, l.getRepaymentDate());
            ps.setString(5, l.getLoanStatus());
            ps.setLong(6, l.getLoanId());
            ps.executeUpdate();
        }
    }

    public static void delete(long loanId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM LOAN_RECORD WHERE LOAN_ID = ?")) {
            ps.setLong(1, loanId);
            ps.executeUpdate();
        }
    }

    private static LoanRecord map(ResultSet rs) throws SQLException {
        return new LoanRecord(
                rs.getLong("LOAN_ID"),
                rs.getLong("MEMBER_ID"),
                rs.getString("MEMBER_NAME"),
                rs.getDouble("LOAN_AMOUNT"),
                rs.getDate("DISBURSEMENT_DATE"),
                rs.getDate("REPAYMENT_DATE"),
                rs.getString("LOAN_STATUS"));
    }
}

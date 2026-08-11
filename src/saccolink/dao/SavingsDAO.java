package saccolink.dao;

import saccolink.db.DBConnection;
import saccolink.db.Jdbc;
import saccolink.model.SavingsRecord;
import saccolink.model.SavingsReport;
import saccolink.session.Session;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Data access for the SAVINGS_RECORD table. */
public class SavingsDAO {

    public static List<SavingsRecord> findAll(Long memberId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT SAVINGS_ID, MEMBER_ID, MEMBER_NAME, CONTRIBUTION_MONTH, "
                + "AMOUNT_CONTRIBUTED FROM V_MY_SAVINGS WHERE 1 = 1");
        if (memberId != null) {
            sql.append(" AND MEMBER_ID = ?");
        }
        sql.append(" ORDER BY CONTRIBUTION_MONTH DESC");

        List<SavingsRecord> list = new ArrayList<>();
        try (Connection c = Session.openScopedConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            if (memberId != null) {
                ps.setLong(1, memberId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /**
     * Inserts a savings record. The CONTRIBUTION_MONTH is normalised to the
     * first day of its month to satisfy the DB's month-truncation check.
     * Returns the SAVINGS_ID assigned by the TRG_SAVINGS_BI trigger.
     */
    public static long insert(SavingsRecord s) throws SQLException {
        String sql = "INSERT INTO SAVINGS_RECORD (MEMBER_ID, CONTRIBUTION_MONTH, AMOUNT_CONTRIBUTED) "
                + "VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, s.getMemberId());
            ps.setDate(2, firstOfMonth(s.getContributionMonth()));
            ps.setDouble(3, s.getAmountContributed());
            ps.executeUpdate();
            return Jdbc.currval(c, "SEQ_SAVINGS");
        }
    }

    public static void delete(long savingsId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM SAVINGS_RECORD WHERE SAVINGS_ID = ?")) {
            ps.setLong(1, savingsId);
            ps.executeUpdate();
        }
    }

    /**
     * Savings report: one row per member with totals, computed with a GROUP BY
     * on the V_MY_SAVINGS view (scoped per logged-in user).
     */
    public static List<SavingsReport> report(Long memberId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT MEMBER_ID, MEMBER_NAME, COUNT(*) AS MONTHS, "
                + "NVL(SUM(AMOUNT_CONTRIBUTED), 0) AS TOTAL, "
                + "ROUND(AVG(AMOUNT_CONTRIBUTED), 2) AS AVG_MONTH, "
                + "TO_CHAR(MIN(CONTRIBUTION_MONTH), 'MON-YYYY') AS FIRST_MONTH, "
                + "TO_CHAR(MAX(CONTRIBUTION_MONTH), 'MON-YYYY') AS LAST_MONTH "
                + "FROM V_MY_SAVINGS WHERE 1 = 1");
        if (memberId != null) {
            sql.append(" AND MEMBER_ID = ?");
        }
        sql.append(" GROUP BY MEMBER_ID, MEMBER_NAME ORDER BY TOTAL DESC");

        List<SavingsReport> list = new ArrayList<>();
        try (Connection c = Session.openScopedConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            if (memberId != null) {
                ps.setLong(1, memberId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SavingsReport(
                            rs.getLong("MEMBER_ID"),
                            rs.getString("MEMBER_NAME"),
                            rs.getInt("MONTHS"),
                            rs.getBigDecimal("TOTAL"),
                            rs.getBigDecimal("AVG_MONTH"),
                            rs.getString("FIRST_MONTH"),
                            rs.getString("LAST_MONTH")));
                }
            }
        }
        return list;
    }

    /** Truncates a date to the first day of its month. */
    public static Date firstOfMonth(Date d) {
        if (d == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Date(cal.getTimeInMillis());
    }

    private static SavingsRecord map(ResultSet rs) throws SQLException {
        return new SavingsRecord(
                rs.getLong("SAVINGS_ID"),
                rs.getLong("MEMBER_ID"),
                rs.getString("MEMBER_NAME"),
                rs.getDate("CONTRIBUTION_MONTH"),
                rs.getDouble("AMOUNT_CONTRIBUTED"));
    }
}

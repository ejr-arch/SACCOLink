package saccolink.dao;

import saccolink.db.DBConnection;
import saccolink.db.Jdbc;
import saccolink.model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Data access for the MEMBER table. */
public class MemberDAO {

    public static Member findById(long id) throws SQLException {
        String sql = "SELECT * FROM MEMBER WHERE MEMBER_ID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public static List<Member> findAll() throws SQLException {
        String sql = "SELECT * FROM MEMBER ORDER BY MEMBER_ID";
        List<Member> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Inserts a new member and returns the generated MEMBER_ID.
     * The PK is assigned by the TRG_MEMBER_BI trigger from SEQ_MEMBER.
     * Validates the 14-character NIN on the client side (DB enforces it too).
     */
    public static long insert(Member m) throws SQLException {
        validate(m);
        String sql = "INSERT INTO MEMBER (NIN, FULL_NAME, PHONE_NUMBER, DISTRICT, CONSENT_GIVEN) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getNin().toUpperCase());
            ps.setString(2, m.getFullName());
            ps.setString(3, m.getPhoneNumber());
            ps.setString(4, m.getDistrict());
            ps.setString(5, m.getConsentFlag());
            ps.executeUpdate();
            return Jdbc.currval(c, "SEQ_MEMBER");
        }
    }

    public static void update(Member m) throws SQLException {
        validate(m);
        String sql = "UPDATE MEMBER SET NIN = ?, FULL_NAME = ?, PHONE_NUMBER = ?, "
                + "DISTRICT = ?, CONSENT_GIVEN = ? WHERE MEMBER_ID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getNin().toUpperCase());
            ps.setString(2, m.getFullName());
            ps.setString(3, m.getPhoneNumber());
            ps.setString(4, m.getDistrict());
            ps.setString(5, m.getConsentFlag());
            ps.setLong(6, m.getMemberId());
            ps.executeUpdate();
        }
    }

    public static void delete(long id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM MEMBER WHERE MEMBER_ID = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static void validate(Member m) {
        String nin = m.getNin() == null ? "" : m.getNin().trim();
        if (!nin.matches("[A-Za-z0-9]{14}")) {
            throw new IllegalArgumentException(
                    "NIN must be exactly 14 characters (NIRA format), letters and digits only.");
        }
        if (m.getFullName() == null || m.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (m.getPhoneNumber() == null || m.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required.");
        }
        if (m.getDistrict() == null || m.getDistrict().trim().isEmpty()) {
            throw new IllegalArgumentException("District is required.");
        }
    }

    private static Member map(ResultSet rs) throws SQLException {
        return new Member(
                rs.getLong("MEMBER_ID"),
                rs.getString("NIN"),
                rs.getString("FULL_NAME"),
                rs.getString("PHONE_NUMBER"),
                rs.getString("DISTRICT"),
                "Y".equalsIgnoreCase(rs.getString("CONSENT_GIVEN")),
                rs.getTimestamp("CREATED_AT"));
    }
}

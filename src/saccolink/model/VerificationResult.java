package saccolink.model;

/** Result of verifying a QR token against FN_VERIFY_PASSPORT. */
public class VerificationResult {
    public static final String VALID = "VALID";
    public static final String EXPIRED = "EXPIRED";
    public static final String REVOKED = "REVOKED";
    public static final String NOT_FOUND = "NOT_FOUND";

    private final String status;
    private final String memberName;
    private final Double scoreValue;
    private final String scoreBand;
    private final java.sql.Timestamp generatedAt;
    private final java.sql.Timestamp expiresAt;

    public VerificationResult(String status, String memberName, Double scoreValue,
                              String scoreBand, java.sql.Timestamp generatedAt,
                              java.sql.Timestamp expiresAt) {
        this.status = status;
        this.memberName = memberName;
        this.scoreValue = scoreValue;
        this.scoreBand = scoreBand;
        this.generatedAt = generatedAt;
        this.expiresAt = expiresAt;
    }

    public String getStatus() { return status; }
    public String getMemberName() { return memberName; }
    public Double getScoreValue() { return scoreValue; }
    public String getScoreBand() { return scoreBand; }
    public java.sql.Timestamp getGeneratedAt() { return generatedAt; }
    public java.sql.Timestamp getExpiresAt() { return expiresAt; }

    public boolean isValid() { return VALID.equals(status); }
}

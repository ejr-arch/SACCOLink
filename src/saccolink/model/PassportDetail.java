package saccolink.model;

import java.sql.Timestamp;

/**
 * Row of the read-only view V_PASSPORT_DETAIL (credit passport joined to
 * member and score), used on the passport log page.
 */
public class PassportDetail {
    private Long passportId;
    private String qrToken;
    private String passportStatus;
    private Timestamp generatedAt;
    private Timestamp expiresAt;
    private long viewCount;
    private Long memberId;
    private String memberName;
    private String nin;
    private String district;
    private double scoreValue;
    private String scoreBand;

    public Long getPassportId() { return passportId; }
    public void setPassportId(Long passportId) { this.passportId = passportId; }

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }

    public String getPassportStatus() { return passportStatus; }
    public void setPassportStatus(String passportStatus) { this.passportStatus = passportStatus; }

    public Timestamp getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Timestamp generatedAt) { this.generatedAt = generatedAt; }

    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }

    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getNin() { return nin; }
    public void setNin(String nin) { this.nin = nin; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public double getScoreValue() { return scoreValue; }
    public void setScoreValue(double scoreValue) { this.scoreValue = scoreValue; }

    public String getScoreBand() { return scoreBand; }
    public void setScoreBand(String scoreBand) { this.scoreBand = scoreBand; }
}

package saccolink.model;

import java.sql.Timestamp;

/** Mirrors the MEMBER table. */
public class Member {
    private Long memberId;
    private String nin;
    private String fullName;
    private String phoneNumber;
    private String district;
    private boolean consentGiven;
    private Timestamp createdAt;

    public Member() {
    }

    public Member(Long memberId, String nin, String fullName,
                  String phoneNumber, String district, boolean consentGiven,
                  Timestamp createdAt) {
        this.memberId = memberId;
        this.nin = nin;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.district = district;
        this.consentGiven = consentGiven;
        this.createdAt = createdAt;
    }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getNin() { return nin; }
    public void setNin(String nin) { this.nin = nin; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public boolean isConsentGiven() { return consentGiven; }
    public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getConsentFlag() { return consentGiven ? "Y" : "N"; }

    @Override
    public String toString() {
        return fullName == null ? super.toString()
                : memberId + " - " + fullName;
    }
}

package saccolink.model;

import java.sql.Date;

/** Mirrors the SAVINGS_RECORD table. */
public class SavingsRecord {
    private Long savingsId;
    private Long memberId;
    private String memberName;
    private Date contributionMonth;
    private double amountContributed;

    public SavingsRecord() {
    }

    public SavingsRecord(Long savingsId, Long memberId, String memberName,
                         Date contributionMonth, double amountContributed) {
        this.savingsId = savingsId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.contributionMonth = contributionMonth;
        this.amountContributed = amountContributed;
    }

    public Long getSavingsId() { return savingsId; }
    public void setSavingsId(Long savingsId) { this.savingsId = savingsId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public Date getContributionMonth() { return contributionMonth; }
    public void setContributionMonth(Date contributionMonth) { this.contributionMonth = contributionMonth; }

    public double getAmountContributed() { return amountContributed; }
    public void setAmountContributed(double amountContributed) { this.amountContributed = amountContributed; }
}

package saccolink.model;

import java.sql.Timestamp;

/** Mirrors the CREDIT_SCORE table (joined to MEMBER for display). */
public class CreditScore {
    private Long scoreId;
    private Long memberId;
    private String memberName;
    private double scoreValue;
    private String scoreBand;
    private Double repaymentScore;
    private Double savingsScore;
    private Timestamp computedAt;
    private boolean current;

    public CreditScore() {
    }

    public Long getScoreId() { return scoreId; }
    public void setScoreId(Long scoreId) { this.scoreId = scoreId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public double getScoreValue() { return scoreValue; }
    public void setScoreValue(double scoreValue) { this.scoreValue = scoreValue; }

    public String getScoreBand() { return scoreBand; }
    public void setScoreBand(String scoreBand) { this.scoreBand = scoreBand; }

    public Double getRepaymentScore() { return repaymentScore; }
    public void setRepaymentScore(Double repaymentScore) { this.repaymentScore = repaymentScore; }

    public Double getSavingsScore() { return savingsScore; }
    public void setSavingsScore(Double savingsScore) { this.savingsScore = savingsScore; }

    public Timestamp getComputedAt() { return computedAt; }
    public void setComputedAt(Timestamp computedAt) { this.computedAt = computedAt; }

    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }
}

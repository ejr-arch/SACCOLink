package saccolink.model;

/**
 * Result of the SACCO creditworthiness check (PKG_SACCOINK_REPORT.MEMBER_SUMMARY).
 * Used when reviewing a loan request before approving / rejecting it.
 */
public class MemberSummary {
    private Long memberId;
    private String memberName;
    private long loanCount;
    private double totalSavings;
    private Double score;
    private String band;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public long getLoanCount() { return loanCount; }
    public void setLoanCount(long loanCount) { this.loanCount = loanCount; }

    public double getTotalSavings() { return totalSavings; }
    public void setTotalSavings(double totalSavings) { this.totalSavings = totalSavings; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getBand() { return band; }
    public void setBand(String band) { this.band = band; }

    public boolean hasScore() {
        return score != null;
    }
}

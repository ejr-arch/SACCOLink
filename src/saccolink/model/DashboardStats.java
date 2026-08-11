package saccolink.model;

/** Key totals shown on the Dashboard (SACCO-wide or per logged-in member). */
public class DashboardStats {

    private long memberCount;
    private long loanCount;
    private double totalOutstanding;
    private double totalSavings;
    private long scoredMembers;
    private double avgScore;
    private long pendingRequests;
    private long passportCount;
    private Double scoreValue;
    private String scoreBand;

    public long getMemberCount() { return memberCount; }
    public void setMemberCount(long v) { memberCount = v; }

    public long getLoanCount() { return loanCount; }
    public void setLoanCount(long v) { loanCount = v; }

    public double getTotalOutstanding() { return totalOutstanding; }
    public void setTotalOutstanding(double v) { totalOutstanding = v; }

    public double getTotalSavings() { return totalSavings; }
    public void setTotalSavings(double v) { totalSavings = v; }

    public long getScoredMembers() { return scoredMembers; }
    public void setScoredMembers(long v) { scoredMembers = v; }

    public double getAvgScore() { return avgScore; }
    public void setAvgScore(double v) { avgScore = v; }

    public long getPendingRequests() { return pendingRequests; }
    public void setPendingRequests(long v) { pendingRequests = v; }

    public long getPassportCount() { return passportCount; }
    public void setPassportCount(long v) { passportCount = v; }

    public Double getScoreValue() { return scoreValue; }
    public void setScoreValue(Double v) { scoreValue = v; }

    public String getScoreBand() { return scoreBand; }
    public void setScoreBand(String v) { scoreBand = v; }
}

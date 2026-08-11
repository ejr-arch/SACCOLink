package saccolink.model;

import java.math.BigDecimal;

/** One row of the savings report: totals grouped per member. */
public class SavingsReport {
    private final long memberId;
    private final String memberName;
    private final int months;
    private final BigDecimal total;
    private final BigDecimal avgPerMonth;
    private final String firstMonth;
    private final String lastMonth;

    public SavingsReport(long memberId, String memberName, int months,
                         BigDecimal total, BigDecimal avgPerMonth,
                         String firstMonth, String lastMonth) {
        this.memberId = memberId;
        this.memberName = memberName;
        this.months = months;
        this.total = total;
        this.avgPerMonth = avgPerMonth;
        this.firstMonth = firstMonth;
        this.lastMonth = lastMonth;
    }

    public long getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public int getMonths() { return months; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getAvgPerMonth() { return avgPerMonth; }
    public String getFirstMonth() { return firstMonth; }
    public String getLastMonth() { return lastMonth; }
}

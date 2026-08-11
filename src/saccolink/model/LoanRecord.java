package saccolink.model;

import java.sql.Date;

/** Mirrors the LOAN_RECORD table. */
public class LoanRecord {
    private Long loanId;
    private Long memberId;
    private String memberName;
    private double loanAmount;
    private Date disbursementDate;
    private Date repaymentDate;
    private String loanStatus;

    public LoanRecord() {
    }

    public LoanRecord(Long loanId, Long memberId, String memberName, double loanAmount,
                      Date disbursementDate, Date repaymentDate, String loanStatus) {
        this.loanId = loanId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.loanAmount = loanAmount;
        this.disbursementDate = disbursementDate;
        this.repaymentDate = repaymentDate;
        this.loanStatus = loanStatus;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public double getLoanAmount() { return loanAmount; }
    public void setLoanAmount(double loanAmount) { this.loanAmount = loanAmount; }

    public Date getDisbursementDate() { return disbursementDate; }
    public void setDisbursementDate(Date disbursementDate) { this.disbursementDate = disbursementDate; }

    public Date getRepaymentDate() { return repaymentDate; }
    public void setRepaymentDate(Date repaymentDate) { this.repaymentDate = repaymentDate; }

    public String getLoanStatus() { return loanStatus; }
    public void setLoanStatus(String loanStatus) { this.loanStatus = loanStatus; }
}

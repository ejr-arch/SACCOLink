package saccolink.model;

import java.sql.Timestamp;

/** Mirrors one row of LOAN_REQUEST (member asks, SACCO reviews). */
public class LoanRequest {
    private Long requestId;
    private Long memberId;
    private String memberName;
    private double requestedAmount;
    private String purpose;
    private Timestamp requestedAt;
    private String status;       // PENDING / APPROVED / REJECTED
    private String reviewedBy;
    private Timestamp reviewedAt;

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public double getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(double requestedAmount) { this.requestedAmount = requestedAmount; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public Timestamp getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Timestamp requestedAt) { this.requestedAt = requestedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public Timestamp getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Timestamp reviewedAt) { this.reviewedAt = reviewedAt; }
}

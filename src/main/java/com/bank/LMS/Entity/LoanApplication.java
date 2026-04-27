package com.bank.LMS.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "application_no", nullable = false, length = 30, unique = true)
    private String applicationNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_la_customer")
    )
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_type_id", foreignKey = @ForeignKey(name = "fk_la_loan_type"))
    private LoanType loanType;

    @Column(name = "amount_requested", precision = 12, scale = 2)
    private BigDecimal amountRequested;

    @Column(name = "tenure_months_requested")
    private Integer tenureMonthsRequested;

    @Column(name = "purpose", length = 200)
    private String purpose;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "status_code",
            referencedColumnName = "status_code",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_la_status")
    )
    private LoanApplicationStatus status;

    @Column(name = "officer_notes", length = 1000)
    private String officerNotes;

    @Column(name = "manager_notes", length = 1000)
    private String managerNotes;

    @Column(name = "needs_info_message", length = 600)
    private String needsInfoMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "needs_info_by_staff_id",
            foreignKey = @ForeignKey(name = "fk_la_needs_info_staff")
    )
    private StaffUsers needsInfoByStaff;

    @Column(name = "needs_info_at")
    private LocalDateTime needsInfoAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // ✅ New timeline tracking fields
    @Column(name = "officer_review_started_at")
    private LocalDateTime officerReviewStartedAt;

    @Column(name = "forwarded_to_risk_at")
    private LocalDateTime forwardedToRiskAt;

    @Column(name = "risk_recommended_approve_at")
    private LocalDateTime riskRecommendedApproveAt;

    @Column(name = "risk_recommended_reject_at")
    private LocalDateTime riskRecommendedRejectAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "employer_name", length = 120)
    private String employerName;

    @Column(name = "designation", length = 80)
    private String designation;

    @Column(name = "monthly_income", precision = 12, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "cibil_score")
    private Integer cibilScore;

    @Column(name = "existing_emis", precision = 12, scale = 2)
    private BigDecimal existingEmis;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "offered_amount", precision = 15, scale = 2)
    private BigDecimal offeredAmount;

    @Column(name = "offered_interest_rate_annual", precision = 5, scale = 2)
    private BigDecimal offeredInterestRateAnnual;

    @Column(name = "offered_tenure_months")
    private Integer offeredTenureMonths;

    @Column(name = "offer_status", length = 30)
    private String offerStatus; // PENDING, ACCEPTED, REJECTED

    @Column(name = "offer_sent_at")
    private LocalDateTime offerSentAt;

    @Column(name = "customer_offer_responded_at")
    private LocalDateTime customerOfferRespondedAt;

    @ManyToOne
    @JoinColumn(name = "offer_sent_by_manager_id")
    private StaffUsers offerSentByManager;

    @Column(name = "offer_rejection_reason", length = 500)
    private String offerRejectionReason;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
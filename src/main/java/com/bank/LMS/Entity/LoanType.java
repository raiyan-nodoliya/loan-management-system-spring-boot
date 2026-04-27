package com.bank.LMS.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "loan_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_type_id")
    private Long loanTypeId;

    @Column(name = "loan_type_code", nullable = false, unique = true, length = 30)
    private String loanTypeCode;

    @Column(name = "loan_type_name", nullable = false, length = 80)
    private String loanTypeName;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "min_monthly_income", precision = 12, scale = 2)
    private BigDecimal minMonthlyIncome;

    @Column(name = "max_amount", precision = 12, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "max_tenure_months")
    private Integer maxTenureMonths;

    @Column(name = "interest_rate_min", precision = 5, scale = 2)
    private BigDecimal interestRateMin;

    @Column(name = "interest_rate_max", precision = 5, scale = 2)
    private BigDecimal interestRateMax;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "loanType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanTypeTenure> tenures;

    @PrePersist
    protected void onCreate() {
        if (active == null) active = true;
        createdAt = LocalDateTime.now();
    }
}
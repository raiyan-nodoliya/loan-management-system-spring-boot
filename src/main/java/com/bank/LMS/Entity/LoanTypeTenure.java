package com.bank.LMS.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loan_type_tenures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanTypeTenure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tenureId;

    @ManyToOne
    @JoinColumn(name = "loan_type_id")
    private LoanType loanType;

    @Column(name = "tenure_months")
    private Integer tenureMonths;
}
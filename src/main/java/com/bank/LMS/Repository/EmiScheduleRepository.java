package com.bank.LMS.Repository;

import com.bank.LMS.Entity.EmiSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EmiScheduleRepository extends JpaRepository<EmiSchedule, Long> {

    List<EmiSchedule> findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(Long loanAccountId);

    long countByLoanAccount_LoanAccountId(Long loanAccountId);

    void deleteByLoanAccount_LoanAccountId(Long loanAccountId);

    Optional<EmiSchedule> findByEmiIdAndLoanAccount_Customer_CustomerId(Long emiId, Long customerId);

    /**
     * FEATURE: Customer ki sabhi active loan accounts ki PENDING installments ka total sum nikalne ke liye.
     * Ye Risk Evaluation mein DTI/FOIR count karne mein kaam aata hai.
     */
    @Query("SELECT SUM(e.emiAmount) FROM EmiSchedule e " +
            "WHERE e.loanAccount.customer.customerId = :customerId " +
            "AND e.status = 'PENDING'")
    BigDecimal sumPendingEmisByCustomerId(@Param("customerId") Long customerId);

    /**
     * Optional: Agar aapko specific loan account ki pending EMI chahiye ho
     */
    @Query("SELECT SUM(e.emiAmount) FROM EmiSchedule e " +
            "WHERE e.loanAccount.loanAccountId = :loanAccountId " +
            "AND e.status = 'PENDING'")
    BigDecimal sumPendingEmisByLoanAccountId(@Param("loanAccountId") Long loanAccountId);
}
package com.bank.LMS.Repository;

import com.bank.LMS.Entity.LoanTypeTenure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanTypeTenureRepository extends JpaRepository<LoanTypeTenure, Long> {

    List<LoanTypeTenure> findByLoanType_LoanTypeId(Long loanTypeId);

}
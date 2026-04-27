package com.bank.LMS.Service.admin.settings;

import com.bank.LMS.Entity.LoanType;
import com.bank.LMS.Entity.LoanTypeTenure;
import com.bank.LMS.Repository.LoanTypeRepository;
import com.bank.LMS.Repository.LoanTypeTenureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class LoanRuleService {

    private final LoanTypeRepository loanTypeRepository;
    @Autowired
    private LoanTypeTenureRepository tenureRepo;

    public LoanRuleService(LoanTypeRepository loanTypeRepository) {

        this.loanTypeRepository = loanTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<LoanType> getAllLoanTypes() {
        List<LoanType> list = loanTypeRepository.findAll();
        System.out.println("SERVICE -> loan types count = " + list.size());
        for (LoanType x : list) {
            System.out.println("SERVICE -> ID=" + x.getLoanTypeId() + ", NAME=" + x.getLoanTypeName());
        }
        return list;
    }

    @Transactional(readOnly = true)
    public LoanType getById(Long id) {
        return loanTypeRepository.findById(id).orElse(null);
    }

    public void saveRule(Long loanTypeId, BigDecimal minMonthlyIncome, BigDecimal maxAmount,
                         Integer maxTenureMonths, BigDecimal interestRateMin, BigDecimal interestRateMax,
                         String customTenures) {

        LoanType loanType = loanTypeRepository.findById(loanTypeId)
                .orElseThrow(() -> new RuntimeException("Loan type not found with id: " + loanTypeId));

        loanType.setMinMonthlyIncome(minMonthlyIncome);
        loanType.setMaxAmount(maxAmount);
        loanType.setMaxTenureMonths(maxTenureMonths);
        loanType.setInterestRateMin(interestRateMin);
        loanType.setInterestRateMax(interestRateMax);

        loanTypeRepository.save(loanType);

        // Delete old tenures first
        List<LoanTypeTenure> old = tenureRepo.findByLoanType_LoanTypeId(loanTypeId);
        tenureRepo.deleteAll(old);

        // Add new tenures
        if (customTenures != null && !customTenures.isBlank()) {
            String[] monthsArray = customTenures.split(",");
            for (String m : monthsArray) {
                LoanTypeTenure tt = new LoanTypeTenure();
                tt.setLoanType(loanType);
                tt.setTenureMonths(Integer.parseInt(m.trim()));
                tenureRepo.save(tt);
            }
        }
    }
}
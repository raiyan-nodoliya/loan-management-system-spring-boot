package com.bank.LMS.Controller.admin.settings;

import com.bank.LMS.Entity.LoanType;
import com.bank.LMS.Service.admin.settings.LoanRuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/loan_rules")
public class AdminLoanRuleController {

    private final LoanRuleService loanRuleService;

    public AdminLoanRuleController(LoanRuleService loanRuleService) {
        this.loanRuleService = loanRuleService;
    }

    @GetMapping
    public String loanRulesPage(@RequestParam(required = false) Long editId, Model model) {
        List<LoanType> loanTypes = loanRuleService.getAllLoanTypes();
        model.addAttribute("loanTypes", loanTypes);

        LoanType selectedLoanType = null;
        if (editId != null) {
            selectedLoanType = loanRuleService.getById(editId);
        }

        if (selectedLoanType == null) {
            selectedLoanType = new LoanType();
        }

        model.addAttribute("selectedLoanType", selectedLoanType);
        return "admin/loan_rules";
    }

    @PostMapping("/save")
    public String saveRule(@RequestParam Long loanTypeId,
                           @RequestParam BigDecimal minMonthlyIncome,
                           @RequestParam BigDecimal maxAmount,
                           @RequestParam Integer maxTenureMonths,
                           @RequestParam BigDecimal interestRateMin,
                           @RequestParam BigDecimal interestRateMax,
                           @RequestParam(required = false) String customTenures, // Naya field
                           RedirectAttributes ra) {

        try {
            if (loanTypeId == null || loanTypeId <= 0) {
                throw new IllegalArgumentException("Please select a valid loan type");
            }

            if (minMonthlyIncome == null || minMonthlyIncome.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Min monthly income must be 0 or greater");
            }

            if (maxAmount == null || maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Max amount must be greater than 0");
            }

            if (maxTenureMonths == null || maxTenureMonths <= 0) {
                throw new IllegalArgumentException("Max tenure must be greater than 0");
            }

            if (interestRateMin == null || interestRateMin.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Minimum interest rate cannot be negative");
            }

            if (interestRateMax == null || interestRateMax.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Maximum interest rate cannot be negative");
            }

            if (interestRateMin.compareTo(interestRateMax) > 0) {
                throw new IllegalArgumentException("Minimum interest rate cannot be greater than maximum interest rate");
            }

            loanRuleService.saveRule(
                    loanTypeId, minMonthlyIncome, maxAmount,
                    maxTenureMonths, interestRateMin, interestRateMax,
                    customTenures // Service mein pass karo
            );

            ra.addFlashAttribute("success", "Loan rule & tenures saved successfully");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/loan_rules";
    }
}
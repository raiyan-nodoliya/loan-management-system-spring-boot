package com.bank.LMS.Controller.customer;

import com.bank.LMS.Entity.Customer;
import com.bank.LMS.Repository.CustomerRepository;
import com.bank.LMS.Repository.LoanDocumentRepository;
import com.bank.LMS.Repository.LoanTypeRepository;
import com.bank.LMS.Service.customer.LoanApplicationService;
import com.bank.LMS.Service.config.MailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

@Controller
@RequestMapping("/apply")
public class CustomerLoanApplyController {

    private final LoanApplicationService loanService;
    private final LoanTypeRepository loanTypeRepo;
    private final CustomerRepository customerRepo;
    private final LoanDocumentRepository docRepo;
    private final MailService mailService;

    public CustomerLoanApplyController(LoanApplicationService loanService,
                                       LoanTypeRepository loanTypeRepo,
                                       CustomerRepository customerRepo,
                                       LoanDocumentRepository docRepo,
                                       MailService mailService) {
        this.loanService = loanService;
        this.loanTypeRepo = loanTypeRepo;
        this.customerRepo = customerRepo;
        this.docRepo = docRepo;
        this.mailService = mailService;
    }

    private Customer getLoggedCustomer(HttpSession session) {
        Long customerId = (Long) session.getAttribute("CUSTOMER_ID");
        return (customerId == null) ? null : customerRepo.findById(customerId).orElse(null);
    }

    @GetMapping("/step1_personal")
    public String step1(HttpSession session, Model model) {
        Customer customer = getLoggedCustomer(session);
        if (customer == null) return "redirect:/customer_login";
        var app = loanService.createOrLoadDraft(customer.getCustomerId(), (Long) session.getAttribute("loanAppId"));
        session.setAttribute("loanAppId", app.getApplicationId());
        model.addAttribute("customer", customer);
        return "customer/apply/step1_personal";
    }

    @PostMapping("/step1_personal")
    public String step1Save(HttpSession session, @RequestParam String name, @RequestParam(required = false) String phone, @RequestParam(required = false) LocalDate dob, @RequestParam(required = false) String address) {
        Customer customer = getLoggedCustomer(session);
        loanService.updateCustomerPersonal(customer.getCustomerId(), name, phone, dob, address);
        return "redirect:/apply/step2_loan";
    }

    @GetMapping("/step2_loan")
    public String step2(HttpSession session, Model model) {
        if (getLoggedCustomer(session) == null) return "redirect:/customer_login";
        model.addAttribute("loanTypes", loanTypeRepo.findByActiveTrueOrderByLoanTypeNameAsc());
        return "customer/apply/step2_loan";
    }

    @PostMapping("/step2_loan")
    public String step2Save(HttpSession session,
                            @RequestParam Long loanTypeId,
                            @RequestParam BigDecimal amountRequested,
                            @RequestParam Integer tenureMonthsRequested,
                            @RequestParam String purpose,
                            @RequestParam String panNumber) { // Added panNumber

        Customer customer = getLoggedCustomer(session);
        Long appId = (Long) session.getAttribute("loanAppId");

        // Save loan details and update PAN in customer profile
        loanService.saveLoanDetails(appId, loanTypeId, amountRequested, tenureMonthsRequested, purpose);
        loanService.updateCustomerPan(customer.getCustomerId(), panNumber);

        return "redirect:/apply/step3_income";
    }
    @GetMapping("/step3_income")
    public String step3(HttpSession session) {
        if (getLoggedCustomer(session) == null) return "redirect:/customer_login";
        return "customer/apply/step3_income";
    }

    @PostMapping("/step3_income")
    public String step3Save(HttpSession session, @RequestParam String employerName, @RequestParam String designation, @RequestParam BigDecimal monthlyIncome, @RequestParam Integer experienceYears) {
        Customer customer = getLoggedCustomer(session);
        Long appId = (Long) session.getAttribute("loanAppId");
        loanService.saveIncomeEmployment(appId, employerName, designation, monthlyIncome, experienceYears);

        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        session.setAttribute("EMAIL_OTP", otp);
        try { mailService.sendApplicationVerificationOtp(customer.getEmail(), otp); } catch (Exception e) { e.printStackTrace(); }

        return "redirect:/apply/step4_documents";
    }

    @GetMapping("/step4_documents")
    public String step4(HttpSession session, Model model) {
        Customer customer = getLoggedCustomer(session);
        if (customer == null) return "redirect:/customer_login";
        String[] docTypes = {"PAN", "AADHAAR", "INCOME_PROOF", "BANK_STATEMENT", "ADDRESS_PROOF"};
        for (String type : docTypes) {
            model.addAttribute("has" + type, docRepo.findTopByCustomer_CustomerIdAndDocumentTypeAndIsLatestTrueOrderByUploadedAtDesc(customer.getCustomerId(), type).isPresent());
        }
        return "customer/apply/step4_documents";
    }

    @PostMapping("/step4_documents")
    public String submit(HttpSession session,
                         @RequestParam(required = false) MultipartFile panFile,
                         @RequestParam(required = false) MultipartFile aadhaarFile,
                         @RequestParam(required = false) MultipartFile incomeProofFile,
                         @RequestParam(required = false) MultipartFile bankStmtFile,
                         @RequestParam(required = false) MultipartFile addressProofFile,
                         @RequestParam String otpInput,
                         RedirectAttributes ra, Model model) {

        // OTP Verification
        if (!otpInput.equals(session.getAttribute("EMAIL_OTP"))) {
            model.addAttribute("otpError", "Invalid OTP");
            return step4(session, model);
        }

        Long appId = (Long) session.getAttribute("loanAppId");
        try {
            // Saare Documents Upload Karo (Agar null nahi hain toh)
            if (panFile != null && !panFile.isEmpty()) loanService.uploadDocument(appId, "PAN", panFile);
            if (aadhaarFile != null && !aadhaarFile.isEmpty()) loanService.uploadDocument(appId, "AADHAAR", aadhaarFile);
            if (incomeProofFile != null && !incomeProofFile.isEmpty()) loanService.uploadDocument(appId, "INCOME_PROOF", incomeProofFile);
            if (bankStmtFile != null && !bankStmtFile.isEmpty()) loanService.uploadDocument(appId, "BANK_STATEMENT", bankStmtFile);
            if (addressProofFile != null && !addressProofFile.isEmpty()) loanService.uploadDocument(appId, "ADDRESS_PROOF", addressProofFile);

            // NOTE: calculateCibilScore(appId) yahan se HATA diya hai. Ab ye Officer karega.

            loanService.submitApplication(appId);

            ra.addFlashAttribute("toastMessage", "Success! Application submitted for review.");
            session.removeAttribute("loanAppId");
            return "redirect:/customer/dashboard";
        } catch (Exception e) {
            model.addAttribute("uploadError", e.getMessage());
            return "customer/apply/step4_documents";
        }
    }
}
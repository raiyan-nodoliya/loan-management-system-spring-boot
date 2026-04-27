package com.bank.LMS.Controller.officer;

import com.bank.LMS.Entity.LoanAccount;
import com.bank.LMS.Entity.LoanApplication;
import com.bank.LMS.Entity.RiskAssessment;
import com.bank.LMS.Entity.StaffUsers;
import com.bank.LMS.Entity.ApplicationMessage; // Added
import com.bank.LMS.Repository.ApplicationMessageRepository; // Added
import com.bank.LMS.Repository.StaffUsersRepository;
import com.bank.LMS.Service.officer.BranchManagerService;
import com.bank.LMS.Service.officer.StaffPermissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired; // Added
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List; // Added

@Controller
@RequestMapping("/manager")
public class BranchManagerController {

    private final BranchManagerService branchManagerService;
    private final StaffUsersRepository staffUsersRepository;
    private final StaffPermissionService staffPermissionService;

    @Autowired // Repository inject ki gayi hai
    private ApplicationMessageRepository messageRepo;

    public BranchManagerController(BranchManagerService branchManagerService,
                                   StaffUsersRepository staffUsersRepository,
                                   StaffPermissionService staffPermissionService) {
        this.branchManagerService = branchManagerService;
        this.staffUsersRepository = staffUsersRepository;
        this.staffPermissionService = staffPermissionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal, HttpSession session) {

        String username = principal.getName();
        StaffUsers user = staffUsersRepository.findByEmail(username).orElse(null);
        String displayName = (user != null) ? user.getName() : username;

        model.addAttribute("loggedInName", displayName);
        model.addAttribute("canViewReports", staffPermissionService.canViewReports(username));

        Object toastMsg = session.getAttribute("toastMessage");
        Object toastType = session.getAttribute("toastType");

        if (toastMsg != null) {
            model.addAttribute("toastMessage", toastMsg);
            model.addAttribute("toastType", toastType);

            session.removeAttribute("toastMessage");
            session.removeAttribute("toastType");
        }

        model.addAttribute("recommendedApproveCount", branchManagerService.recommendedApproveCount());
        model.addAttribute("recommendedRejectCount", branchManagerService.recommendedRejectCount());
        model.addAttribute("approvedTodayCount", branchManagerService.approvedTodayCount());
        model.addAttribute("disbursalPendingCount", branchManagerService.disbursalPendingCount());
        model.addAttribute("applications", branchManagerService.dashboardApplications());

        return "manager/dashboard";
    }

    @GetMapping("/decision/{id}")
    public String decisionPage(@PathVariable Long id,
                               Model model,
                               Principal principal,
                               RedirectAttributes ra) {

        LoanApplication app = branchManagerService.getApplication(id);
        if (app == null) {
            ra.addFlashAttribute("toastMessage", "Loan application not found");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/manager/dashboard";
        }

        // --- Message Fetching Logic ---
        List<ApplicationMessage> messages = messageRepo.findByApplicationIdOrderByCreatedAtAsc(id);
        model.addAttribute("messages", messages);
        // ------------------------------

        String username = principal.getName();
        StaffUsers user = staffUsersRepository.findByEmail(username).orElse(null);
        String displayName = (user != null) ? user.getName() : username;
        model.addAttribute("loggedInName", displayName);

        RiskAssessment assessment = branchManagerService.getRiskAssessment(id);
        LoanAccount existingAccount = branchManagerService.getLoanAccountByApplication(id);

        model.addAttribute("app", app);
        model.addAttribute("riskAssessment", assessment);
        model.addAttribute("existingAccount", existingAccount);

        return "manager/decision";
    }

    @PostMapping("/decision/{id}/approve")
    public String approveLoan(@PathVariable Long id,
                              @RequestParam BigDecimal sanctionAmount,
                              @RequestParam BigDecimal interestRateAnnual,
                              @RequestParam Integer tenureMonths,
                              @RequestParam(required = false) String managerNotes,
                              RedirectAttributes ra) {

        boolean ok = branchManagerService.approveLoan(
                id, sanctionAmount, interestRateAnnual, tenureMonths, managerNotes
        );

        if (ok) {
            ra.addFlashAttribute("toastMessage", "Loan approved and loan account created successfully");
            ra.addFlashAttribute("toastType", "success");
        } else {
            ra.addFlashAttribute("toastMessage", "Loan approval failed");
            ra.addFlashAttribute("toastType", "error");
        }

        return "redirect:/manager/dashboard";
    }

    @PostMapping("/decision/{id}/reject")
    public String rejectLoan(@PathVariable Long id,
                             @RequestParam(required = false) String managerNotes,
                             RedirectAttributes ra) {

        boolean ok = branchManagerService.rejectLoan(id, managerNotes);

        if (ok) {
            ra.addFlashAttribute("toastMessage", "Loan rejected successfully");
            ra.addFlashAttribute("toastType", "success");
        } else {
            ra.addFlashAttribute("toastMessage", "Loan rejection failed");
            ra.addFlashAttribute("toastType", "error");
        }

        return "redirect:/manager/dashboard";
    }


    @PostMapping("/decision/{id}/send-offer")
    public String sendOffer(@PathVariable Long id,
                            @RequestParam BigDecimal sanctionAmount,
                            @RequestParam BigDecimal interestRateAnnual,
                            @RequestParam Integer tenureMonths,
                            @RequestParam(required = false) String managerNotes,
                            RedirectAttributes ra) {

        boolean ok = branchManagerService.sendOfferToCustomer(
                id, sanctionAmount, interestRateAnnual, tenureMonths, managerNotes
        );

        if (ok) {
            ra.addFlashAttribute("toastMessage", "Offer sent to customer successfully");
            ra.addFlashAttribute("toastType", "success");
        } else {
            ra.addFlashAttribute("toastMessage", "Failed to send offer to customer");
            ra.addFlashAttribute("toastType", "error");
        }

        return "redirect:/manager/dashboard";
    }
}
package com.bank.LMS.Controller.officer;

import com.bank.LMS.Entity.LoanApplication;
import com.bank.LMS.Entity.RiskAssessment;
import com.bank.LMS.Entity.StaffUsers;
import com.bank.LMS.Entity.ApplicationMessage;
import com.bank.LMS.Repository.ApplicationMessageRepository;
import com.bank.LMS.Repository.StaffUsersRepository;
import com.bank.LMS.Repository.EmiScheduleRepository; // Naya Repository
import com.bank.LMS.Service.officer.RiskOfficerService;
import com.bank.LMS.Service.officer.StaffPermissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/risk")
public class RiskOfficerDashboardController {

    private final RiskOfficerService riskOfficerService;
    private final StaffUsersRepository staffUsersRepository;
    private final StaffPermissionService staffPermissionService;

    @Autowired
    private ApplicationMessageRepository messageRepo;

    @Autowired
    private EmiScheduleRepository emiScheduleRepo; // EMI fetch karne ke liye

    public RiskOfficerDashboardController(RiskOfficerService riskOfficerService,
                                          StaffUsersRepository staffUsersRepository,
                                          StaffPermissionService staffPermissionService) {
        this.riskOfficerService = riskOfficerService;
        this.staffUsersRepository = staffUsersRepository;
        this.staffPermissionService = staffPermissionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal, HttpSession session) {
        if (principal != null) {
            String username = principal.getName();
            StaffUsers user = staffUsersRepository.findByEmail(username).orElse(null);
            model.addAttribute("loggedInName", user != null ? user.getName() : username);
            model.addAttribute("canViewReports", staffPermissionService.canViewReports(username));
        }

        model.addAttribute("pendingEvaluation", riskOfficerService.pendingEvaluationCount());
        model.addAttribute("highRiskCases", riskOfficerService.highRiskCount());
        model.addAttribute("completedToday", riskOfficerService.completedTodayCount());
        model.addAttribute("applications", riskOfficerService.riskQueue());

        return "risk/dashboard";
    }

    @GetMapping("/evaluate/{id}")
    public String evaluatePage(@PathVariable Long id, Model model, RedirectAttributes ra, Principal principal) {

        LoanApplication app = riskOfficerService.getApplication(id);
        if (app == null) {
            ra.addFlashAttribute("toastMessage", "Application not found");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/risk/dashboard";
        }

        // 1. Pehle se chal rahi messages fetch karo
        model.addAttribute("messages", messageRepo.findByApplicationIdOrderByCreatedAtAsc(id));

        // 2. Logged in user info
        if (principal != null) {
            StaffUsers user = staffUsersRepository.findByEmail(principal.getName()).orElse(null);
            model.addAttribute("loggedInName", user != null ? user.getName() : principal.getName());
        }

        // 3. FEATURE: Existing EMI calculation from EmiSchedule table
        // Hum system se check kar rahe hain ki is customer ki kitni EMI baki hain
        BigDecimal systemPendingEmis = emiScheduleRepo.sumPendingEmisByCustomerId(app.getCustomer().getCustomerId());
        if (systemPendingEmis == null) systemPendingEmis = BigDecimal.ZERO;

        // Agar application form mein pehle se koi EMI likhi hai, toh dono mein se jo zyada ho wo le sakte hain
        // Ya fir sirf system wala dikhao. Yaha hum dono ka logic handle kar rahe hain:
        BigDecimal finalExistingEmis = (app.getExistingEmis() != null && app.getExistingEmis().compareTo(BigDecimal.ZERO) > 0)
                ? app.getExistingEmis() : systemPendingEmis;

        BigDecimal monthlyIncome = (app.getMonthlyIncome() != null) ? app.getMonthlyIncome() : BigDecimal.ZERO;

        // Initial FOIR (DTI) Count
        BigDecimal foir = riskOfficerService.calculateFoir(monthlyIncome, finalExistingEmis);

        model.addAttribute("app", app);
        model.addAttribute("existingAssessment", riskOfficerService.getExistingAssessment(id));
        model.addAttribute("foir", foir);
        model.addAttribute("defaultEmis", finalExistingEmis); // Textbox ke liye

        return "risk/evaluate_application";
    }

    @PostMapping("/evaluate/{id}")
    public String submitEvaluation(@PathVariable Long id,
                                   @RequestParam BigDecimal monthlyIncome,
                                   @RequestParam(defaultValue = "0") BigDecimal existingEmis,
                                   @RequestParam String recommendation,
                                   @RequestParam(required = false) String notes,
                                   RedirectAttributes ra) {

        boolean ok = riskOfficerService.submitEvaluation(id, monthlyIncome, existingEmis, recommendation, notes);

        ra.addFlashAttribute("toastMessage", ok ? "Evaluation submitted" : "Failed to submit");
        ra.addFlashAttribute("toastType", ok ? "success" : "error");

        return ok ? "redirect:/risk/dashboard" : "redirect:/risk/evaluate/" + id;
    }
}
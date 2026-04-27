package com.bank.LMS.Controller.auth;

import com.bank.LMS.Entity.Customer;
import com.bank.LMS.Entity.LoanApplication;
import com.bank.LMS.Entity.LoanDocument;
import com.bank.LMS.Repository.CustomerRepository;
import com.bank.LMS.Repository.EmiScheduleRepository;
import com.bank.LMS.Repository.LoanAccountRepository;
import com.bank.LMS.Repository.LoanApplicationRepository;
import com.bank.LMS.Repository.LoanApplicationStatusRepository;
import com.bank.LMS.Repository.LoanDocumentRepository;
import com.bank.LMS.Service.customer.LoanDocumentStorageService;
import com.bank.LMS.Service.customer.ApplicationTimelineService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer")
public class publicController {

    private final LoanApplicationRepository loanRepo;
    private final CustomerRepository customerRepo;
    private final LoanDocumentRepository docRepo;
    private final LoanApplicationStatusRepository statusRepo;
    private final LoanDocumentStorageService storageService;
    private final LoanAccountRepository loanAccountRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final ApplicationTimelineService applicationTimelineService;

    public publicController(LoanApplicationRepository loanRepo,
                            CustomerRepository customerRepo,
                            LoanDocumentRepository docRepo,
                            LoanApplicationStatusRepository statusRepo,
                            LoanDocumentStorageService storageService,
                            LoanAccountRepository loanAccountRepository,
                            EmiScheduleRepository emiScheduleRepository,
                            ApplicationTimelineService applicationTimelineService) {
        this.loanRepo = loanRepo;
        this.customerRepo = customerRepo;
        this.docRepo = docRepo;
        this.statusRepo = statusRepo;
        this.storageService = storageService;
        this.loanAccountRepository = loanAccountRepository;
        this.emiScheduleRepository = emiScheduleRepository;
        this.applicationTimelineService =applicationTimelineService;
    }

    @GetMapping("/dashboard")
    public String customerDashboard(HttpSession session, RedirectAttributes ra, Model model) {
        Object id = session.getAttribute("CUSTOMER_ID");
        Long customerId = (Long) session.getAttribute("CUSTOMER_ID");

        if (id == null) {
            ra.addFlashAttribute("toastMessage", "Please login first");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/customer_login";
        }

        Customer customer = customerRepo.findById(customerId).orElse(null);

        var applications = loanRepo.findTop5ByCustomer_CustomerIdOrderByApplicationIdDesc(customerId);
        var allApplications = loanRepo.findTop5ByCustomer_CustomerIdOrderByApplicationIdDesc(customerId);

        String name = (String) session.getAttribute("CUSTOMER_NAME");
        if (name == null || name.isBlank()) name = "Customer";

        long totalApplications = (allApplications != null) ? allApplications.size() : 0;

        String latestStatus = "N/A";
        if (allApplications != null && !allApplications.isEmpty()
                && allApplications.get(0).getStatus() != null
                && allApplications.get(0).getStatus().getLabel() != null) {
            latestStatus = allApplications.get(0).getStatus().getLabel();
        }

        long pendingDocuments = 0;
        if (allApplications != null) {
            for (var app : allApplications) {
                if (app.getStatus() != null && "NEEDS_INFO".equals(app.getStatus().getStatusCode())) {
                    pendingDocuments++;
                }
            }
        }

        String nextEmiDate = "N/A";
        String nextEmiAmount = "N/A";

        model.addAttribute("loggedInName", name);
        model.addAttribute("applications", applications);
        model.addAttribute("customer", customer);

        model.addAttribute("totalApplications", totalApplications);
        model.addAttribute("latestStatus", latestStatus);
        model.addAttribute("pendingDocuments", pendingDocuments);
        model.addAttribute("nextEmiDate", nextEmiDate);
        model.addAttribute("nextEmiAmount", nextEmiAmount);

        return "customer/dashboard";
    }

    @GetMapping("/application/{id}")
    public String applicationDetails(@PathVariable("id") Long applicationId,
                                     HttpSession session,
                                     Model model) {

        Long customerId = (Long) session.getAttribute("CUSTOMER_ID");
        if (customerId == null) return "redirect:/customer_login";

        var app = loanRepo.findByApplicationIdAndCustomer_CustomerId(applicationId, customerId)
                .orElse(null);

        if (app == null) return "redirect:/customer/dashboard";

        var documents = docRepo.findByApplication_ApplicationIdAndIsLatestTrueOrderByDocumentTypeAsc(applicationId);

        model.addAttribute("app", app);
        model.addAttribute("documents", documents != null ? documents : java.util.List.of());
        model.addAttribute("timelineSteps", applicationTimelineService.buildTimeline(app));

        return "customer/application_details";
    }

    @GetMapping("/my_applications")
    public String my_applications(Model model, HttpSession session, RedirectAttributes ra) {
        Long customerId = (Long) session.getAttribute("CUSTOMER_ID");
        if (customerId == null) {
            ra.addFlashAttribute("toastMessage", "Please login first");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/customer_login";
        }

        Customer customer = customerRepo.findById(customerId).orElse(null);
        var applications = loanRepo.findTop5ByCustomer_CustomerIdOrderByApplicationIdDesc(customerId);

        model.addAttribute("applications", applications);
        model.addAttribute("customer", customer);

        return "customer/my_applications";
    }

    @GetMapping("/my_loans")
    public String my_loans(Model model, HttpSession session, RedirectAttributes ra) {

        Long customerId = (Long) session.getAttribute("CUSTOMER_ID");
        if (customerId == null) {
            ra.addFlashAttribute("toastMessage", "Please login first");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/customer_login";
        }

        String name = (String) session.getAttribute("CUSTOMER_NAME");
        if (name == null || name.isBlank()) name = "Customer";

        var loans = loanRepo.findByCustomer_CustomerIdAndStatus_StatusCodeInOrderByApplicationIdDesc(
                customerId,
                java.util.List.of("APPROVED", "DISBURSED")
        );

        model.addAttribute("loggedInName", name);
        model.addAttribute("loans", loans);

        return "customer/my_loans";
    }

    @GetMapping("/emi_schedule/{id}")
    public String emi_schedule(@PathVariable("id") Long applicationId,
                               HttpSession session,
                               RedirectAttributes ra,
                               Model model) {

        Long customerId = (Long) session.getAttribute("CUSTOMER_ID");
        if (customerId == null) {
            ra.addFlashAttribute("toastMessage", "Please login first");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/customer_login";
        }

        var appOpt = loanRepo.findByApplicationIdAndCustomer_CustomerId(applicationId, customerId);
        if (appOpt.isEmpty()) {
            ra.addFlashAttribute("toastMessage", "Loan not found");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/customer/my_loans";
        }

        var loanAccount = loanAccountRepository.findByApplication_ApplicationId(applicationId).orElse(null);
        if (loanAccount == null) {
            ra.addFlashAttribute("toastMessage", "Loan account not found");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/customer/my_loans";
        }

        var emis = emiScheduleRepository.findByLoanAccount_LoanAccountIdOrderByInstallmentNoAsc(
                loanAccount.getLoanAccountId()
        );

        model.addAttribute("loanAccount", loanAccount);
        model.addAttribute("emis", emis);

        return "customer/emi_schedule";
    }

//    @GetMapping("/my_profile")
//    public String my_profile() {
//        return "customer/my_profile";
//    }

    @GetMapping("/application/{id}/upload")
    public String uploadDocsPage(@PathVariable Long id,
                                 HttpSession session,
                                 Model model,
                                 RedirectAttributes ra) {

        Long customerId = (Long) session.getAttribute("CUSTOMER_ID");
        if (customerId == null) {
            ra.addFlashAttribute("toastMessage", "Please login first");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/customer_login";
        }

        var app = loanRepo.findByApplicationIdAndCustomer_CustomerId(id, customerId).orElse(null);
        if (app == null) {
            ra.addFlashAttribute("toastMessage", "Application not found");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/customer/my_applications";
        }

        java.util.List<LoanDocument> documents;

        if (app.getNeedsInfoAt() != null) {
            documents = docRepo.findRecentDocs(id, customerId, app.getNeedsInfoAt());
        } else {
            documents = java.util.List.of();
        }

        model.addAttribute("app", app);
        model.addAttribute("documents", documents);

        return "customer/apply/upload_documents";
    }


    @GetMapping("/track")
    public String trackPage(Model model) {
        model.addAttribute("applicationNo", "");
        return "public/track_application";
    }

    @PostMapping("/track")
    public String trackApplication(@RequestParam("applicationNo") String applicationNo,
                                   Model model) {

        String appNo = applicationNo == null ? "" : applicationNo.trim();

        if (appNo.isBlank()) {
            model.addAttribute("notFound", true);
            model.addAttribute("applicationNo", "");
            return "public/track_application";
        }

        LoanApplication app = loanRepo.findByApplicationNo(appNo).orElse(null);

        if (app == null) {
            model.addAttribute("notFound", true);
            model.addAttribute("applicationNo", appNo);
            return "public/track_application";
        }

        var documents = docRepo.findByApplication_ApplicationIdAndIsLatestTrueOrderByDocumentTypeAsc(
                app.getApplicationId()
        );

        model.addAttribute("app", app);
        model.addAttribute("documents", documents != null ? documents : java.util.List.of());
        model.addAttribute("timelineSteps", applicationTimelineService.buildTimeline(app));
        model.addAttribute("applicationNo", appNo);
        model.addAttribute("notFound", false);

        return "public/track_application";
    }




    @PostMapping("/application/{id}/upload")
    @Transactional
    public String uploadDocs(@PathVariable Long id,
                             @RequestParam String documentType,
                             @RequestParam("file") MultipartFile file,
                             HttpSession session,
                             RedirectAttributes ra) {

        Long customerId = (Long) session.getAttribute("CUSTOMER_ID");
        if (customerId == null) {
            ra.addFlashAttribute("toastMessage", "Please login first");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/customer_login";
        }

        var app = loanRepo.findByApplicationIdAndCustomer_CustomerId(id, customerId).orElse(null);
        if (app == null) {
            ra.addFlashAttribute("toastMessage", "Application not found");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/customer/my_applications";
        }

        Customer customer = customerRepo.findById(customerId).orElse(null);
        if (customer == null) {
            ra.addFlashAttribute("toastMessage", "Customer not found");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/customer/my_applications";
        }

        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("toastMessage", "Please select a file");
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/customer/application/" + id + "/upload";
        }

        try {
            String normalizedDocType = documentType == null ? "" : documentType.trim().toUpperCase();

            var previousLatest = docRepo
                    .findTopByApplication_ApplicationIdAndDocumentTypeAndIsLatestTrueOrderByVersionNoDesc(id, normalizedDocType)
                    .orElse(null);

            int nextVersion = previousLatest != null ? previousLatest.getVersionNo() + 1 : 1;

            // pehle file physically store karo
            var stored = storageService.store(file, app.getApplicationNo(), normalizedDocType);

            // store successful ho gaya tabhi old latest false karo
            docRepo.markOldLatestFalse(id, normalizedDocType);

            LoanDocument doc = LoanDocument.builder()
                    .application(app)
                    .customer(customer)
                    .documentType(normalizedDocType)
                    .fileName(stored.fileName())
                    .filePath(stored.filePath())
                    .originalFileName(stored.originalFileName())
                    .mimeType(
                            stored.mimeType() == null || stored.mimeType().isBlank()
                                    ? "application/octet-stream"
                                    : stored.mimeType()
                    )
                    .fileSizeBytes(stored.sizeBytes())
                    .versionNo(nextVersion)
                    .isLatest(true)
                    .status(LoanDocument.DocumentStatus.UPLOADED)
                    .build();

            docRepo.saveAndFlush(doc);

            var submitted = statusRepo.findByStatusCode("SUBMITTED")
                    .orElseThrow(() -> new RuntimeException("Status SUBMITTED missing"));

            app.setStatus(submitted);
            app.setNeedsInfoMessage(null);

            loanRepo.saveAndFlush(app);

            ra.addFlashAttribute("toastMessage", "Document uploaded successfully");
            ra.addFlashAttribute("toastType", "success");
            return "redirect:/customer/application/" + id + "/upload";

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("toastMessage", "Upload failed: " + e.getMessage());
            ra.addFlashAttribute("toastType", "error");
            return "redirect:/customer/application/" + id + "/upload";
        }
    }
}
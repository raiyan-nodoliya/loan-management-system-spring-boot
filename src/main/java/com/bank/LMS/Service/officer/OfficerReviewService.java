package com.bank.LMS.Service.officer;

import com.bank.LMS.Entity.LoanApplication;
import com.bank.LMS.Entity.LoanApplicationStatus;
import com.bank.LMS.Entity.LoanDocument;
import com.bank.LMS.Repository.LoanApplicationRepository;
import com.bank.LMS.Repository.LoanApplicationStatusRepository;
import com.bank.LMS.Repository.LoanDocumentRepository;
import com.bank.LMS.Service.config.MailService;
import com.bank.LMS.Service.customer.LoanApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service handling all Bank Officer operations including
 * application review, PAN-validated CIBIL fetching, and status transitions.
 */
@Service
public class OfficerReviewService {

    private final LoanApplicationRepository loanRepo;
    private final LoanDocumentRepository docRepo;
    private final LoanApplicationStatusRepository statusRepo;
    private final MailService mailService;
    private final LoanApplicationService loanApplicationService;

    public OfficerReviewService(LoanApplicationRepository loanRepo,
                                LoanDocumentRepository docRepo,
                                LoanApplicationStatusRepository statusRepo,
                                MailService mailService,
                                LoanApplicationService loanApplicationService) {
        this.loanRepo = loanRepo;
        this.docRepo = docRepo;
        this.statusRepo = statusRepo;
        this.mailService = mailService;
        this.loanApplicationService = loanApplicationService;
    }

    /**
     * ✅ BUG #4 FIX: PAN Validated CIBIL Fetch
     * Ensures the officer enters the correct PAN before system triggers score calculation.
     */
    @Transactional
    public void fetchCibilWithPan(Long appId, String inputPan) {
        LoanApplication app = loanRepo.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Loan Application not found for ID: " + appId));

        String storedPan = app.getCustomer().getPanCard();

        // 1. Check if PAN exists in record
        if (storedPan == null || storedPan.isBlank()) {
            throw new IllegalArgumentException("Customer has not provided a PAN card number in their profile.");
        }

        // 2. Strict validation against input
        if (!storedPan.equalsIgnoreCase(inputPan.trim())) {
            throw new IllegalArgumentException("PAN Number Mismatch! Verification failed for bureau fetch.");
        }

        // 3. Proceed to calculation if matched
        loanApplicationService.generateCibilScore(appId);
    }

    /**
     * Fetch all applications currently in the Officer's bucket.
     */
    public List<LoanApplication> queue() {
        return loanRepo.findByStatus_StatusCodeInOrderByApplicationIdDesc(
                List.of("SUBMITTED", "NEEDS_INFO", "IN_REVIEW")
        );
    }

    public LoanApplication getApp(Long id) {
        return loanRepo.findById(id).orElse(null);
    }

    /**
     * ✅ BUG #3 FIX: Full Document History
     * Returns all versions of documents for transparency.
     */
    public List<LoanDocument> getDocs(Long appId) {
        return docRepo.findByApplication_ApplicationIdOrderByUploadedAtDesc(appId);
    }

    @Transactional
    public boolean markInReview(Long appId, String notes) {
        LoanApplication app = loanRepo.findById(appId).orElse(null);
        if (app == null) return false;

        LoanApplicationStatus st = statusRepo.findByStatusCode("IN_REVIEW").orElse(null);
        if (st == null) return false;

        app.setStatus(st);
        if (app.getOfficerReviewStartedAt() == null) {
            app.setOfficerReviewStartedAt(LocalDateTime.now());
        }
        if (notes != null && !notes.isBlank()) {
            app.setOfficerNotes(notes);
        }

        loanRepo.save(app);
        sendCustomerMail(app, "Review Started", "Your application is now under official review by our loan officer.");
        return true;
    }

    @Transactional
    public boolean requestInfo(Long appId, String message, String notes) {
        LoanApplication app = loanRepo.findById(appId).orElse(null);
        if (app == null) return false;

        LoanApplicationStatus st = statusRepo.findByStatusCode("NEEDS_INFO").orElse(null);
        if (st == null) return false;

        app.setStatus(st);
        app.setNeedsInfoMessage(message);
        app.setNeedsInfoAt(LocalDateTime.now());

        if (notes != null && !notes.isBlank()) {
            app.setOfficerNotes(notes);
        }

        loanRepo.save(app);
        sendCustomerMail(app, "Action Required", "Information requested: " + message);
        return true;
    }

    @Transactional
    public boolean forwardToRisk(Long id, String officerNotes) {
        LoanApplication app = loanRepo.findById(id).orElse(null);
        if (app == null) return false;

        LoanApplicationStatus st = statusRepo.findByStatusCode("FORWARDED_TO_RISK")
                .orElseGet(() -> statusRepo.findByStatusCode("RISK_EVALUATION").orElse(null));

        if (st == null) return false;

        app.setStatus(st);
        app.setForwardedToRiskAt(LocalDateTime.now());

        if (officerNotes != null && !officerNotes.isBlank()) {
            app.setOfficerNotes(officerNotes);
        }

        loanRepo.save(app);
        sendCustomerMail(app, "Verification Step", "Your application has moved to the Risk Evaluation stage.");
        return true;
    }

    private void sendCustomerMail(LoanApplication app, String actionTitle, String description) {
        if (app == null || app.getCustomer() == null || app.getCustomer().getEmail() == null) return;
        mailService.sendApplicationStatusUpdate(
                app.getCustomer().getEmail(),
                app.getCustomer().getName(),
                app.getApplicationNo(),
                app.getStatus().getStatusCode(),
                actionTitle,
                description
        );
    }

    // --- Dashboard Counter Methods ---
    public long countSubmitted() { return loanRepo.countByStatus_StatusCode("SUBMITTED"); }
    public long countNeedsInfo() { return loanRepo.countByStatus_StatusCode("NEEDS_INFO"); }
    public long countInReview() { return loanRepo.countByStatus_StatusCode("IN_REVIEW"); }

    public LoanApplicationService getLoanService() {
        return this.loanApplicationService;
    }
}
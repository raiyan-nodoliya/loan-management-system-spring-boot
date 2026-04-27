package com.bank.LMS.Service.officer;

import com.bank.LMS.Entity.*;
import com.bank.LMS.Repository.*;
import com.bank.LMS.Service.config.MailService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RiskOfficerService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanApplicationStatusRepository loanApplicationStatusRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskRecommendationStatusRepository riskRecommendationStatusRepository;
    private final StaffUsersRepository staffUsersRepository;
    private final EmiScheduleRepository emiScheduleRepository; // Naya Injection
    private final MailService mailService;

    public RiskOfficerService(LoanApplicationRepository loanApplicationRepository,
                              LoanApplicationStatusRepository loanApplicationStatusRepository,
                              RiskAssessmentRepository riskAssessmentRepository,
                              RiskRecommendationStatusRepository riskRecommendationStatusRepository,
                              StaffUsersRepository staffUsersRepository,
                              EmiScheduleRepository emiScheduleRepository, // Added to Constructor
                              MailService mailService) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.loanApplicationStatusRepository = loanApplicationStatusRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.riskRecommendationStatusRepository = riskRecommendationStatusRepository;
        this.staffUsersRepository = staffUsersRepository;
        this.emiScheduleRepository = emiScheduleRepository;
        this.mailService = mailService;
    }

    @Transactional(readOnly = true)
    public long pendingEvaluationCount() {
        return loanApplicationRepository.countByStatus_StatusCode("RISK_EVALUATION");
    }

    @Transactional(readOnly = true)
    public long highRiskCount() {
        return riskAssessmentRepository.countByRecommendationStatus_StatusCode("HIGH_RISK");
    }

    @Transactional(readOnly = true)
    public long completedTodayCount() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return riskAssessmentRepository.countByAssessedAtBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<LoanApplication> riskQueue() {
        return loanApplicationRepository.findByStatus_StatusCodeOrderByApplicationIdDesc("RISK_EVALUATION");
    }

    @Transactional(readOnly = true)
    public LoanApplication getApplication(Long id) {
        return loanApplicationRepository.findByApplicationId(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public RiskAssessment getExistingAssessment(Long applicationId) {
        LoanApplication app = getApplication(applicationId);
        if (app == null) return null;
        return riskAssessmentRepository.findByApplication(app).orElse(null);
    }

    // FEATURE: Pehle se chal rahi EMIs ka total nikalne ka logic
    @Transactional(readOnly = true)
    public BigDecimal getSystemCalculatedEmis(Long customerId) {
        BigDecimal sum = emiScheduleRepository.sumPendingEmisByCustomerId(customerId);
        return (sum != null) ? sum : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateFoir(BigDecimal monthlyIncome, BigDecimal totalEmis) {
        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (totalEmis == null) {
            totalEmis = BigDecimal.ZERO;
        }

        return totalEmis
                .multiply(BigDecimal.valueOf(100))
                .divide(monthlyIncome, 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public int calculateRiskScore(BigDecimal monthlyIncome,
                                  BigDecimal existingEmis,
                                  BigDecimal requestedAmount) {

        int score = 0;
        BigDecimal foir = calculateFoir(monthlyIncome, existingEmis);

        // FOIR Based Scoring
        if (foir.compareTo(BigDecimal.valueOf(20)) <= 0) {
            score += 20;
        } else if (foir.compareTo(BigDecimal.valueOf(35)) <= 0) {
            score += 12;
        } else if (foir.compareTo(BigDecimal.valueOf(50)) <= 0) {
            score += 8;
        } else {
            score += 3;
        }

        // Income Based Scoring
        if (monthlyIncome != null) {
            if (monthlyIncome.compareTo(BigDecimal.valueOf(100000)) >= 0) {
                score += 25;
            } else if (monthlyIncome.compareTo(BigDecimal.valueOf(50000)) >= 0) {
                score += 15;
            } else {
                score += 8;
            }
        }

        // Loan Amount Based Scoring
        if (requestedAmount != null) {
            if (requestedAmount.compareTo(BigDecimal.valueOf(1000000)) <= 0) {
                score += 25;
            } else if (requestedAmount.compareTo(BigDecimal.valueOf(3000000)) <= 0) {
                score += 15;
            } else {
                score += 8;
            }
        }

        // Default points for stability (Employer/Tenure logic can be added here)
        score += 30;

        return Math.min(score, 100);
    }

    public String autoRiskLevel(int riskScore) {
        if (riskScore >= 75) return "LOW";
        if (riskScore >= 45) return "MEDIUM";
        return "HIGH";
    }

    public boolean submitEvaluation(Long applicationId,
                                    BigDecimal monthlyIncome,
                                    BigDecimal totalEmis,
                                    String recommendation,
                                    String notes) {

        Optional<LoanApplication> appOpt = loanApplicationRepository.findByApplicationId(applicationId);
        if (appOpt.isEmpty()) return false;

        LoanApplication app = appOpt.get();
        StaffUsers currentOfficer = getCurrentStaffUser();
        if (currentOfficer == null) return false;

        // Scoring based on the updated EMIs
        int riskScore = calculateRiskScore(monthlyIncome, totalEmis, app.getAmountRequested());
        String riskLevel = autoRiskLevel(riskScore);

        String rrCode;
        String appStatusCode;

        switch (recommendation) {
            case "APPROVE" -> {
                rrCode = "RECOMMENDED";
                appStatusCode = "RECOMMENDED";
            }
            case "REJECT" -> {
                rrCode = "REJECTED";
                appStatusCode = "REJECTED";
            }
            case "NEED_MORE_INFO" -> {
                rrCode = "NEED_MORE_INFO";
                appStatusCode = "NEEDS_INFO";
            }
            default -> { return false; }
        }

        // Overriding logic if Risk is too high but officer recommends approval
        if ("HIGH".equals(riskLevel) && "APPROVE".equals(recommendation)) {
            rrCode = "HIGH_RISK";
            appStatusCode = "RISK_HOLD";
        }

        RiskRecommendationStatus rrStatus = riskRecommendationStatusRepository.findByStatusCode(rrCode).orElse(null);
        LoanApplicationStatus appStatus = loanApplicationStatusRepository.findByStatusCode(appStatusCode).orElse(null);

        if (rrStatus == null || appStatus == null) return false;

        LocalDateTime now = LocalDateTime.now();
        RiskAssessment assessment = riskAssessmentRepository.findByApplication(app).orElse(new RiskAssessment());

        assessment.setApplication(app);
        assessment.setRiskOfficer(currentOfficer);
        assessment.setRecommendationStatus(rrStatus);
        assessment.setRiskScore(riskScore);
        assessment.setRemarks(notes);
        assessment.setAssessedAt(now);
        riskAssessmentRepository.save(assessment);

        // Update the application with verified income and EMIs
        app.setMonthlyIncome(monthlyIncome);
        app.setExistingEmis(totalEmis);
        app.setStatus(appStatus);

        // Timelines
        if ("APPROVE".equals(recommendation)) {
            if (!"HIGH".equals(riskLevel)) {
                app.setRiskRecommendedApproveAt(now);
            }
        } else if ("REJECT".equals(recommendation)) {
            app.setRiskRecommendedRejectAt(now);
        } else if ("NEED_MORE_INFO".equals(recommendation)) {
            app.setNeedsInfoAt(now);
            app.setNeedsInfoMessage(notes);
        }

        loanApplicationRepository.save(app);

        // Mail Logic
        String description = "Risk evaluation completed. Score: " + riskScore + ", Level: " + riskLevel;
        sendCustomerMail(app, "Risk evaluation update", description);

        return true;
    }

    private void sendCustomerMail(LoanApplication app, String actionTitle, String description) {
        if (app == null || app.getCustomer() == null) return;
        String email = app.getCustomer().getEmail();
        if (email == null || email.isBlank()) return;

        mailService.sendApplicationStatusUpdate(
                email,
                app.getCustomer().getName(),
                app.getApplicationNo(),
                app.getStatus().getStatusCode(),
                actionTitle,
                description
        );
    }

    @Transactional(readOnly = true)
    public StaffUsers getCurrentStaffUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return staffUsersRepository.findByEmail(username).orElse(null);
    }
}
package com.bank.LMS.Service.customer;

import com.bank.LMS.Entity.*;
import com.bank.LMS.Repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository appRepo;
    private final CustomerRepository customerRepo;
    private final LoanTypeRepository loanTypeRepo;
    private final LoanDocumentRepository docRepo;
    private final LoanDocumentStorageService storage;
    private final LoanApplicationStatusRepository loanStatusRepo;

    public LoanApplicationService(LoanApplicationRepository appRepo,
                                  CustomerRepository customerRepo,
                                  LoanTypeRepository loanTypeRepo,
                                  LoanDocumentRepository docRepo,
                                  LoanDocumentStorageService storage,
                                  LoanApplicationStatusRepository loanStatusRepo) {
        this.appRepo = appRepo;
        this.customerRepo = customerRepo;
        this.loanTypeRepo = loanTypeRepo;
        this.docRepo = docRepo;
        this.storage = storage;
        this.loanStatusRepo = loanStatusRepo;
    }

    // --- APPLICATION DRAFT & UPDATE LOGIC ---

    @Transactional
    public LoanApplication createOrLoadDraft(Long customerId, Long existingAppId) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        if (existingAppId != null) {
            LoanApplication existing = appRepo.findById(existingAppId).orElse(null);
            if (existing != null && existing.getCustomer() != null && existing.getCustomer().getCustomerId().equals(customerId)) {
                return existing;
            }
        }

        LoanApplication draft = new LoanApplication();
        draft.setCustomer(customer);
        draft.setApplicationNo(generateApplicationNo());
        draft.setStatus(loanStatusRepo.findByStatusCode("DRAFT").orElseThrow());
        draft.setAmountRequested(BigDecimal.ZERO);
        draft.setTenureMonthsRequested(0);
        draft.setMonthlyIncome(BigDecimal.ZERO);
        draft.setPurpose("");
        draft.setEmployerName("");
        draft.setDesignation("");
        draft.setExperienceYears(0);

        return appRepo.save(draft);
    }

    @Transactional
    public void updateCustomerPersonal(Long customerId, String name, String phone, LocalDate dob, String address) {
        Customer customer = customerRepo.findById(customerId).orElseThrow();
        if (name != null && !name.isBlank()) customer.setName(name);
        if (phone != null && !phone.isBlank()) customer.setPhone(phone);
        if (dob != null) customer.setDob(dob);
        if (address != null && !address.isBlank()) customer.setAddress(address);
        customerRepo.save(customer);
    }

    @Transactional
    public void saveLoanDetails(Long appId, Long loanTypeId, BigDecimal amountRequested, Integer tenureMonthsRequested, String purpose) {
        LoanApplication app = appRepo.findById(appId).orElseThrow();
        app.setLoanType(loanTypeRepo.findById(loanTypeId).orElseThrow());
        app.setAmountRequested(amountRequested);
        app.setTenureMonthsRequested(tenureMonthsRequested);
        app.setPurpose(purpose);
        appRepo.save(app);
    }

    @Transactional
    public void updateCustomerPan(Long customerId, String panNumber) {
        Customer customer = customerRepo.findById(customerId).orElseThrow();
        if (panNumber != null && !panNumber.isBlank()) {
            customer.setPanCard(panNumber.toUpperCase().trim());
            customerRepo.save(customer);
        }
    }

    @Transactional
    public void saveIncomeEmployment(Long appId, String employerName, String designation, BigDecimal monthlyIncome, Integer experienceYears) {
        LoanApplication app = appRepo.findById(appId).orElseThrow();
        app.setEmployerName(employerName);
        app.setDesignation(designation);
        app.setMonthlyIncome(monthlyIncome);
        app.setExperienceYears(experienceYears);
        appRepo.save(app);
    }

    // --- BUG #3 FIX: DOCUMENT VERSIONING LOGIC ---

    @Transactional
    public void uploadDocument(Long appId, String docType, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) return;

        LoanApplication app = appRepo.findById(appId).orElseThrow();
        Long custId = app.getCustomer().getCustomerId();

        // 1. Current Max Version dhoondho (Customer level par)
        Integer currentMax = docRepo.findMaxVersionByCustomer(custId, docType);
        int nextVersion = (currentMax == null) ? 1 : currentMax + 1;

        // 2. Is Customer ke is Type ke purane saare documents 'isLatest = false' mark karo
        docRepo.markAllOldVersionsAsNotLatest(custId, docType);

        // 3. Naya file storage mein save karo
        var stored = storage.store(file, app.getApplicationNo(), docType);

        // 4. Database mein naya entry create karo (Latest flag ke sath)
        LoanDocument doc = LoanDocument.builder()
                .application(app)
                .customer(app.getCustomer())
                .documentType(docType)
                .fileName(stored.fileName())
                .filePath(stored.filePath())
                .originalFileName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .versionNo(nextVersion)
                .isLatest(true)
                .status(LoanDocument.DocumentStatus.UPLOADED)
                .build();

        docRepo.save(doc);
    }

    // --- BUG #4 FIX: OFFICER-DRIVEN CIBIL SCORE ---

    /**
     * Ye method ab Customer Controller se CALL NAHI HOGA.
     * Isko Officer Controller call karega.
     */
    @Transactional
    public void generateCibilScore(Long appId) {
        LoanApplication app = appRepo.findById(appId).orElseThrow();

        // Banking Logic Simulation
        int score = 650; // Base Score

        // Salary Factor
        if (app.getMonthlyIncome().compareTo(new BigDecimal("50000")) > 0) score += 100;
        else if (app.getMonthlyIncome().compareTo(new BigDecimal("25000")) > 0) score += 50;

        // Experience Factor
        if (app.getExperienceYears() >= 5) score += 100;
        else if (app.getExperienceYears() >= 2) score += 50;

        app.setCibilScore(Math.min(score, 850));
        appRepo.save(app);
        // Note: Save hone ke baad ye teeno (Officer, Risk, Manager) ko automatically dikhega.
    }

    @Transactional
    public void submitApplication(Long appId) {
        LoanApplication app = appRepo.findById(appId).orElseThrow();
        validateBeforeSubmit(app);

        // Change status to SUBMITTED
        app.setStatus(loanStatusRepo.findByStatusCode("SUBMITTED").orElseThrow());
        app.setSubmittedAt(LocalDateTime.now());

        appRepo.saveAndFlush(app);
    }

    private void validateBeforeSubmit(LoanApplication app) {
        if (app.getLoanType() == null || app.getAmountRequested().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Missing mandatory loan details");
        }
        // Basic Check: Kya PAN aur Aadhaar uploaded hain?
        // (Iska logic aap docRepo.countByApplicationAndIsLatestTrue se add kar sakte hain)
    }

    private String generateApplicationNo() {
        return "LA-" + LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
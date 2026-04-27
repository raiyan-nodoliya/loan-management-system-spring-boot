package com.bank.LMS.Repository;

import com.bank.LMS.Entity.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoanDocumentRepository extends JpaRepository<LoanDocument, Long> {

    // Versioning Helpers
    @Query("SELECT MAX(d.versionNo) FROM LoanDocument d WHERE d.customer.customerId = :custId AND d.documentType = :docType")
    Integer findMaxVersionByCustomer(@Param("custId") Long custId, @Param("docType") String docType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE LoanDocument d SET d.isLatest = false WHERE d.customer.customerId = :custId AND d.documentType = :docType AND d.isLatest = true")
    int markAllOldVersionsAsNotLatest(@Param("custId") Long custId, @Param("docType") String docType);

    // Methods used by PublicController & Officer
    List<LoanDocument> findByApplication_ApplicationIdOrderByUploadedAtDesc(Long appId);
    List<LoanDocument> findByApplication_ApplicationIdAndIsLatestTrueOrderByDocumentTypeAsc(Long appId);

    // COMPATIBILITY FIX: publicController is looking for this exact name
    Optional<LoanDocument> findTopByApplication_ApplicationIdAndDocumentTypeAndIsLatestTrueOrderByVersionNoDesc(Long appId, String docType);

    // COMPATIBILITY FIX: CustomerLoanApplyController is looking for this
    Optional<LoanDocument> findTopByCustomer_CustomerIdAndDocumentTypeAndIsLatestTrueOrderByUploadedAtDesc(Long custId, String docType);

    List<LoanDocument> findByCustomer_CustomerIdAndIsLatestTrueOrderByDocumentTypeAsc(Long customerId);

    @Query("SELECT d FROM LoanDocument d WHERE d.application.applicationId = :appId " +
            "AND d.customer.customerId = :custId AND d.isLatest = true " +
            "AND d.uploadedAt >= :since ORDER BY d.documentType ASC")
    List<LoanDocument> findRecentDocs(@Param("appId") Long appId, @Param("custId") Long custId, @Param("since") LocalDateTime since);

    @Modifying(clearAutomatically = true)
    @Query("update LoanDocument d set d.isLatest = false where d.application.applicationId = :appId and d.documentType = :docType")
    int markOldLatestFalse(@Param("appId") Long appId, @Param("docType") String docType);
}
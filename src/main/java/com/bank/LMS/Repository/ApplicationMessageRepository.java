package com.bank.LMS.Repository;

import com.bank.LMS.Entity.ApplicationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationMessageRepository extends JpaRepository<ApplicationMessage, Long> {

    List<ApplicationMessage> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);

}
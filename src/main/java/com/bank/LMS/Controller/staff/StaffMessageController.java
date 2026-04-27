package com.bank.LMS.Controller.staff;


import com.bank.LMS.Entity.ApplicationMessage;
import com.bank.LMS.Repository.ApplicationMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class StaffMessageController {

    @Autowired
    private ApplicationMessageRepository messageRepo;

    @PostMapping("/staff/message/send")
    public String sendMessage(
            @RequestParam Long applicationId,
            @RequestParam String receiverRole,
            @RequestParam String message,
            Principal principal,
            HttpServletRequest request) { // Request object add karein

        ApplicationMessage m = new ApplicationMessage();
        m.setApplicationId(applicationId);

        // Yahan principal.getName() user ka username dega (e.g. "admin" ya "officer1")
        // Agar aapko "BANK_OFFICER" jaisa role set karna hai toh custom logic lagega
        m.setSenderRole(principal.getName());

        m.setReceiverRole(receiverRole);
        m.setMessage(message);

        messageRepo.save(m);

        // Ye line user ko wapis wahi bhej degi jahan se wo aaya tha
        String referer = request.getHeader("Referer");
        return "redirect:" + referer;
    }
}
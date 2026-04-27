package com.bank.LMS.Service.config;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendRegistrationSuccess(String toEmail, String name) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setSubject("LoanHub - Registration Successful");
            msg.setText(
                    "Hi " + name + ",\n\n" +
                            "Your registration was successful.\n" +
                            "You can now login and access your dashboard.\n\n" +
                            "Thanks,\nLoanHub Team"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            System.out.println("MAIL ERROR: " + e.getMessage());
        }
    }

    public void sendForgotPasswordOtp(String toEmail, String otp) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setSubject("LoanHub - Password Reset OTP");
            msg.setText(
                    "Hello,\n\n" +
                            "Your LoanHub password reset OTP is: " + otp + "\n\n" +
                            "This OTP is valid for 5 minutes.\n" +
                            "Please do not share this OTP with anyone.\n\n" +
                            "Thanks,\nLoanHub Team"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            System.out.println("FORGOT PASSWORD OTP MAIL ERROR: " + e.getMessage());
        }
    }

    public void sendApplicationStatusUpdate(String toEmail,
                                            String customerName,
                                            String applicationNo,
                                            String statusCode,
                                            String actionTitle,
                                            String description) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setSubject("LoanHub - Application Update [" + applicationNo + "]");

            String safeDescription = (description != null && !description.isBlank())
                    ? description
                    : "No additional description provided.";

            msg.setText(
                    "Hi " + customerName + ",\n\n" +
                            "Your loan application has been updated.\n\n" +
                            "Application No : " + applicationNo + "\n" +
                            "Status Code    : " + statusCode + "\n" +
                            "Action         : " + actionTitle + "\n" +
                            "Description    : " + safeDescription + "\n\n" +
                            "Please keep your Application No safe so you can track your application easily.\n\n" +
                            "Thanks,\nLoanHub Team"
            );

            mailSender.send(msg);

        } catch (Exception e) {
            System.out.println("APPLICATION STATUS MAIL ERROR: " + e.getMessage());
        }
    }

    public void sendNewStaffCreatedToOfficers(List<String> officerEmails,
                                              String createdStaffName,
                                              String createdStaffEmail,
                                              String roleName) {
        try {
            if (officerEmails == null || officerEmails.isEmpty()) {
                return;
            }

            for (String officerEmail : officerEmails) {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(officerEmail);
                msg.setSubject("LoanHub - New Staff User Created");
                msg.setText(
                        "Hello Officer,\n\n" +
                                "A new staff user has been created in LoanHub.\n\n" +
                                "Name  : " + createdStaffName + "\n" +
                                "Email : " + createdStaffEmail + "\n" +
                                "Role  : " + roleName + "\n\n" +
                                "Please login to the system if you need to review or coordinate further.\n\n" +
                                "Thanks,\nLoanHub Team"
                );
                mailSender.send(msg);
            }

        } catch (Exception e) {
            System.out.println("NEW STAFF OFFICER MAIL ERROR: " + e.getMessage());
        }
    }

    public void sendStaffAccountCreated(String toEmail,
                                        String name,
                                        String roleName) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setSubject("LoanHub - Staff Account Created");
            msg.setText(
                    "Hi " + name + ",\n\n" +
                            "Your staff account has been created successfully in LoanHub.\n\n" +
                            "Role : " + roleName + "\n\n" +
                            "You can now login using your registered email.\n\n" +
                            "Thanks,\nLoanHub Team"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            System.out.println("STAFF ACCOUNT CREATED MAIL ERROR: " + e.getMessage());
        }
    }


    // MailService.java mein ye method add karo
    public void sendApplicationVerificationOtp(String toEmail, String otp) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setSubject("LoanHub - Application Verification OTP");
            msg.setText(
                    "Hello,\n\n" +
                            "Thank you for applying with LoanHub.\n" +
                            "Your verification OTP to submit the application is: " + otp + "\n\n" +
                            "This OTP is valid for 10 minutes.\n\n" +
                            "Thanks,\nLoanHub Team"
            );
            mailSender.send(msg);
        } catch (Exception e) {
            System.out.println("APPLICATION OTP MAIL ERROR: " + e.getMessage());
        }
    }
}
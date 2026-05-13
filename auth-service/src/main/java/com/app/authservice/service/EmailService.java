package com.app.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    public boolean sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(mailUsername);
            message.setTo(toEmail);
            message.setSubject("CodeSync Password Reset OTP");
            message.setText(
                    "Hello,\n\n" +
                    "Your OTP is: " + otp + "\n\n" +
                    "Valid for 10 minutes.\n\n" +
                    "CodeSync Team"
            );

            javaMailSender.send(message);
            System.out.println("OTP email sent successfully to: " + toEmail);
            return true;

        } catch (MailAuthenticationException e) {
            System.out.println("MAIL ERROR: Gmail authentication failed. Check email and app password. " + e.getMessage());
            return false;
        } catch (MailException e) {
            System.out.println("MAIL ERROR: Unable to send OTP email. " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("MAIL ERROR: Unexpected mail failure. " + e.getMessage());
            return false;
        }
    }
}

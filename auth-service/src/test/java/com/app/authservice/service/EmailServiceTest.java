package com.app.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(javaMailSender);
        ReflectionTestUtils.setField(emailService, "mailUsername", "noreply@codesync.test");
    }

    @Test
    void sendOtpEmailSendsExpectedMessage() {
        boolean sent = emailService.sendOtpEmail("krishna@example.com", "123456");

        assertThat(sent).isTrue();

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@codesync.test");
        assertThat(message.getTo()).containsExactly("krishna@example.com");
        assertThat(message.getSubject()).isEqualTo("CodeSync Password Reset OTP");
        assertThat(message.getText()).contains("Your OTP is: 123456");
    }

    @Test
    void sendOtpEmailReturnsFalseWhenMailAuthenticationFails() {
        doThrow(new MailAuthenticationException("bad credentials"))
                .when(javaMailSender)
                .send(any(SimpleMailMessage.class));

        boolean sent = emailService.sendOtpEmail("krishna@example.com", "123456");

        assertThat(sent).isFalse();
    }
}

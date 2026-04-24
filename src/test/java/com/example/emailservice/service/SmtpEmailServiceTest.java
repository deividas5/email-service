package com.example.emailservice.service;

import com.example.emailservice.dto.EmailRequest;
import com.example.emailservice.exception.EmailSendException;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    private SmtpEmailService emailService;

    @BeforeEach
    void setUp() {
        // Use a real (no-op) RetryRegistry so @PostConstruct doesn't fail
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        emailService = new SmtpEmailService(mailSender, templateEngine, retryRegistry);
        emailService.configureRetryLogging();
    }

    @Test
    void sendEmail_success() throws MessagingException {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(IContext.class)))
                .thenReturn("<html><body>Welcome, John!</body></html>");

        EmailRequest request = new EmailRequest(
                "john@example.com",
                "Welcome!",
                "welcome",
                Map.of("name", "John", "message", "Hello World"));

        emailService.sendEmail(request);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendEmail_throwsEmailSendException_onMailException() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(IContext.class)))
                .thenReturn("<html><body>Welcome!</body></html>");
        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        EmailRequest request = new EmailRequest(
                "fail@example.com",
                "Subject",
                "welcome",
                Map.of("name", "X", "message", "Y"));

        assertThatThrownBy(() -> emailService.sendEmail(request))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("fail@example.com");
    }

    @Test
    void sendEmail_usesEmptyMapWhenTemplateVariablesNull() throws MessagingException {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(IContext.class)))
                .thenReturn("<html><body>Hi!</body></html>");

        // null templateVariables is normalised to empty map by the record compact constructor
        EmailRequest request = new EmailRequest("a@b.com", "Hi", "welcome", null);

        assertThat(request.templateVariables()).isEmpty();
        emailService.sendEmail(request);
        verify(mailSender).send(any(MimeMessage.class));
    }
}

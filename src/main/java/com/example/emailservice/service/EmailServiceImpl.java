package com.example.emailservice.service;

import com.example.emailservice.dto.EmailRequest;
import com.example.emailservice.exception.EmailSendException;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final RetryRegistry retryRegistry;

    public EmailServiceImpl(JavaMailSender mailSender,
                            SpringTemplateEngine templateEngine,
                            RetryRegistry retryRegistry) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.retryRegistry = retryRegistry;
    }

    @PostConstruct
    void configureRetryLogging() {
        retryRegistry.retry("emailRetry")
                .getEventPublisher()
                .onRetry(event -> log.warn(
                        "Retrying email send — attempt {}/3, cause: {}",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable().getMessage()));
    }

    @Override
    @Retry(name = "emailRetry")
    public void sendEmail(EmailRequest request) {
        log.info("Sending email to '{}' using template '{}'", request.to(), request.templateName());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, StandardCharsets.UTF_8.name());

            helper.setTo(request.to());
            helper.setSubject(request.subject());

            Context context = new Context();
            context.setVariables(request.templateVariables());
            String html = templateEngine.process("emails/" + request.templateName(), context);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Email delivered successfully to '{}'", request.to());

        } catch (MessagingException | MailException e) {
            log.warn("Email to '{}' failed: {}", request.to(), e.getMessage());
            throw new EmailSendException(
                    "Failed to send email to '" + request.to() + "'", e);
        }
    }
}

package com.example.emailservice.controller;

import com.example.emailservice.dto.EmailRequest;
import com.example.emailservice.dto.EmailResponse;
import com.example.emailservice.dto.ErrorResponse;
import com.example.emailservice.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/emails")
@Tag(name = "Emails", description = "Send transactional emails via SMTP templates")
public class EmailController {

    private static final Logger log = LoggerFactory.getLogger(EmailController.class);

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @Operation(
        summary = "Send an email",
        description = "Renders the named Thymeleaf template with the supplied variables and delivers it via SMTP."
    )
    @ApiResponse(responseCode = "200", description = "Email accepted and delivered",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = EmailResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failure (invalid address, blank fields)",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "SMTP delivery failed after retries",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<EmailResponse> sendEmail(@Valid @RequestBody EmailRequest request) {
        log.info("POST /api/v1/emails — to='{}', template='{}'", request.to(), request.templateName());
        emailService.sendEmail(request);
        return ResponseEntity.ok(new EmailResponse("Email sent successfully", request.to(), request.subject()));
    }

    @Operation(summary = "Service health", description = "Lightweight liveness check for the email service.")
    @ApiResponse(responseCode = "200", description = "Service is up")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "email-service"));
    }
}

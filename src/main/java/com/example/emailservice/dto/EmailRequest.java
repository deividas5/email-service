package com.example.emailservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record EmailRequest(
        @Email(message = "Must be a valid email address")
        @NotBlank(message = "Recipient email is required")
        String to,

        @NotBlank(message = "Subject is required")
        String subject,

        @NotBlank(message = "Template name is required")
        String templateName,

        Map<String, Object> templateVariables
) {
    public EmailRequest {
        templateVariables = templateVariables != null ? templateVariables : Map.of();
    }
}

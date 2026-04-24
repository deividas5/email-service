package com.example.emailservice.dto;

public record EmailResponse(
        String message,
        String to,
        String subject
) {}

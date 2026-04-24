package com.example.emailservice.service;

import com.example.emailservice.dto.EmailRequest;

public interface EmailService {

    void sendEmail(EmailRequest request);
}

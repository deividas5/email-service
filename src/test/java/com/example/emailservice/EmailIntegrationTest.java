package com.example.emailservice;

import com.example.emailservice.dto.EmailRequest;
import com.example.emailservice.dto.EmailResponse;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "spring.mail.username=",
        "spring.mail.password=",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false"
})
class EmailIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withPerMethodLifecycle(false);

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void sendEmail_returnsOkAndDeliversMessage() throws Exception {
        EmailRequest request = new EmailRequest(
                "recipient@example.com",
                "Integration Test Subject",
                "welcome",
                Map.of("name", "Alice", "message", "Integration test is passing!"));

        ResponseEntity<EmailResponse> response = restTemplate.postForEntity(
                "/api/v1/emails", request, EmailResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().to()).isEqualTo("recipient@example.com");
        assertThat(response.getBody().message()).isEqualTo("Email sent successfully");

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo("Integration Test Subject");
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("recipient@example.com");
    }

    @Test
    void sendEmail_returnsBadRequest_onInvalidEmail() {
        Map<String, Object> body = Map.of(
                "to", "not-an-email",
                "subject", "Hello",
                "templateName", "welcome");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/emails", body, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("to");
    }

    @Test
    void sendEmail_returnsBadRequest_onBlankSubject() {
        Map<String, Object> body = Map.of(
                "to", "user@example.com",
                "subject", "",
                "templateName", "welcome");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/emails", body, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("subject");
    }

    @Test
    void healthEndpoint_returnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/emails/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
}

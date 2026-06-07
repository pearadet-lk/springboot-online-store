package com.onlinestore.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinestore.contracts.Dtos.EmailNotificationRequestedEvent;
import com.onlinestore.contracts.Dtos.EmailSendStatusDto;
import com.onlinestore.emailservice.EmailServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest(classes = EmailServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmailKafkaIT {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("bitnamilegacy/kafka:3.8.1-debian-12-r0"));

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("messaging.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void consumerProcessesEmailNotification() throws Exception {
        var orderId = UUID.randomUUID();
        var evt = new EmailNotificationRequestedEvent(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                "demo@example.com",
                "Demo",
                new BigDecimal("10.00"),
                "USD",
                OffsetDateTime.now(ZoneOffset.UTC));

        kafkaTemplate.send("email-notifications", objectMapper.writeValueAsString(evt)).get(10, TimeUnit.SECONDS);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var response = restTemplate.getForEntity("/email/status/" + orderId, EmailSendStatusDto.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo("Sent");
        });
    }
}

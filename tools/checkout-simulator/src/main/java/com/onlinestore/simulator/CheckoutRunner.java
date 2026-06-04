package com.onlinestore.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.onlinestore.contracts.DemoConstants;
import com.onlinestore.contracts.Dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class CheckoutRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CheckoutRunner.class);

    private final SimulatorProperties simulator;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Value("${gateway.base-url:http://localhost:8081/}")
    private String gatewayBaseUrl;

    @Value("${messaging.kafka.topic:email-notifications}")
    private String kafkaTopic;

    public CheckoutRunner(SimulatorProperties simulator, KafkaTemplate<String, String> kafkaTemplate) {
        this.simulator = simulator;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        var base = gatewayBaseUrl.endsWith("/") ? gatewayBaseUrl : gatewayBaseUrl + "/";
        var client = RestClient.builder()
                .baseUrl(base)
                .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build()))
                .build();

        waitForGateway(client);

        var login = client.post()
                .uri("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserLoginRequest(DemoConstants.DEMO_EMAIL, DemoConstants.DEMO_PASSWORD))
                .retrieve()
                .body(LoginResponse.class);
        if (login == null) {
            throw new IllegalStateException("Login failed");
        }

        var authed = client.mutate().defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + login.accessToken()).build();

        var products = authed.get().uri("/api/products").retrieve().body(ProductDto[].class);
        if (products == null || products.length == 0) {
            throw new IllegalStateException("No products");
        }
        var product = products[0];
        var userId = login.user().userId();

        authed.put()
                .uri("/api/carts/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpsertCartRequest(userId, List.of(new CartItemDto(product.productId(), 1, product.price()))))
                .retrieve()
                .toBodilessEntity();

        authed.post()
                .uri("/api/inventory/{id}/reserve?quantity=1", product.productId())
                .retrieve()
                .toBodilessEntity();

        var order = authed.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateOrderRequest(userId, "USD", List.of(new CartItemDto(product.productId(), 1, product.price()))))
                .retrieve()
                .body(OrderDto.class);

        authed.post()
                .uri("/api/payments/authorize")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AuthorizePaymentRequest(
                        order.orderId(), order.totalAmount(), order.currency(), "tok_demo"))
                .retrieve()
                .toBodilessEntity();

        authed.post()
                .uri("/api/inventory/{id}/commit?quantity=1", product.productId())
                .retrieve()
                .toBodilessEntity();

        authed.post().uri("/api/orders/{id}/complete", order.orderId()).retrieve().toBodilessEntity();
        authed.post().uri("/api/shipments/{id}", order.orderId()).retrieve().toBodilessEntity();

        authed.post()
                .uri("/api/history/events")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "historyId", UUID.randomUUID(),
                        "orderId", order.orderId(),
                        "userId", userId,
                        "eventType", "CheckoutCompleted",
                        "createdAt", OffsetDateTime.now(ZoneOffset.UTC),
                        "notes", "checkout-simulator"))
                .retrieve()
                .toBodilessEntity();

        if (simulator.publishKafka()) {
            var evt = new EmailNotificationRequestedEvent(
                    UUID.randomUUID(),
                    order.orderId(),
                    userId,
                    login.user().email(),
                    login.user().fullName(),
                    order.totalAmount(),
                    order.currency(),
                    OffsetDateTime.now(ZoneOffset.UTC));
            kafkaTemplate.send(kafkaTopic, mapper.writeValueAsString(evt)).get();
            log.info("Kafka message published to topic {}", kafkaTopic);
        } else {
            log.info("Kafka publish skipped (simulator.publish-kafka=false).");
        }

        log.info("Checkout simulation completed for order {}", order.orderId());
        System.exit(0);
    }

    private void waitForGateway(RestClient client) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            try {
                client.get().uri("/health").retrieve().toBodilessEntity();
                return;
            } catch (Exception ignored) {
                Thread.sleep(2000);
            }
        }
        throw new IllegalStateException("Gateway not reachable at " + gatewayBaseUrl);
    }
}

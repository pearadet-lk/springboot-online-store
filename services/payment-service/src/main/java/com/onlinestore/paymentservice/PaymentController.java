package com.onlinestore.paymentservice;

import com.onlinestore.contracts.Dtos.AuthorizePaymentRequest;
import com.onlinestore.contracts.Dtos.PaymentDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class PaymentController {

    private final Map<UUID, PaymentDto> paymentsByOrder = new ConcurrentHashMap<>();
    private final Map<String, PaymentDto> idempotency = new ConcurrentHashMap<>();
    private final StripeProperties stripe;

    public PaymentController(StripeProperties stripe) {
        this.stripe = stripe;
        if (stripe.secretKey() != null && !stripe.secretKey().isBlank()) {
            com.stripe.Stripe.apiKey = stripe.secretKey();
        }
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "payment-service", "status", "ok");
    }

    @GetMapping("/api/v1/health")
    public Map<String, String> healthV1() {
        return health();
    }

    @PostMapping("/payments/authorize")
    public PaymentDto authorize(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody AuthorizePaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required.");
        }
        var cached = idempotency.get(idempotencyKey);
        if (cached != null) {
            return cached;
        }
        var providerRef = (stripe.secretKey() != null && !stripe.secretKey().isBlank())
                ? "stripe-mock-" + UUID.randomUUID()
                : "mock-" + UUID.randomUUID();
        var payment = new PaymentDto(
                UUID.randomUUID(),
                request.orderId(),
                request.amount(),
                request.currency(),
                "Authorized",
                providerRef,
                OffsetDateTime.now(ZoneOffset.UTC));
        paymentsByOrder.put(request.orderId(), payment);
        idempotency.put(idempotencyKey, payment);
        return payment;
    }

    @GetMapping("/payments/{orderId}")
    public PaymentDto get(@PathVariable UUID orderId) {
        var payment = paymentsByOrder.get(orderId);
        if (payment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return payment;
    }

    @PostMapping("/payments/{orderId}/void")
    public PaymentDto voidPayment(@PathVariable UUID orderId) {
        var existing = paymentsByOrder.get(orderId);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        var voided = new PaymentDto(
                existing.paymentId(),
                existing.orderId(),
                existing.amount(),
                existing.currency(),
                "Voided",
                existing.providerReference(),
                OffsetDateTime.now(ZoneOffset.UTC));
        paymentsByOrder.put(orderId, voided);
        return voided;
    }
}

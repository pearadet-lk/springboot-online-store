package com.onlinestore.contracts;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class Dtos {

    private Dtos() {}

    public record CartItemDto(UUID productId, int quantity, BigDecimal unitPrice) {}

    public record CheckoutRequest(UUID userId, String currency, List<CartItemDto> items) {}

    public record CreateOrderRequest(UUID userId, String currency, List<CartItemDto> items) {}

    public record OrderDto(
            UUID orderId,
            UUID userId,
            BigDecimal totalAmount,
            String currency,
            String status,
            OffsetDateTime createdAt) {}

    public record AuthorizePaymentRequest(
            UUID orderId, BigDecimal amount, String currency, String paymentMethodToken) {}

    public record PaymentDto(
            UUID paymentId,
            UUID orderId,
            BigDecimal amount,
            String currency,
            String status,
            String providerReference,
            OffsetDateTime createdAt) {}

    public record ProductDto(
            UUID productId, String name, String description, BigDecimal price, boolean isActive) {}

    public record UpsertCartRequest(UUID userId, List<CartItemDto> items) {}

    public record CartDto(UUID cartId, UUID userId, List<CartItemDto> items, OffsetDateTime updatedAt) {}

    public record UserProfileDto(UUID userId, String email, String fullName, OffsetDateTime createdAt) {}

    public record UserRegistrationRequest(String email, String password, String fullName) {}

    public record UserLoginRequest(String email, String password) {}

    public record RefreshTokenRequest(String refreshToken) {}

    public record InventoryItemDto(
            UUID productId, int availableQty, int reservedQty, OffsetDateTime updatedAt) {}

    public record ShipmentDto(
            UUID shipmentId,
            UUID orderId,
            String carrier,
            String trackingNumber,
            String status,
            OffsetDateTime createdAt) {}

    public record OrderHistoryEventDto(
            UUID historyId,
            UUID orderId,
            UUID userId,
            String eventType,
            OffsetDateTime createdAt,
            String notes) {}

    public record EmailNotificationRequestedEvent(
            UUID notificationId,
            UUID orderId,
            UUID userId,
            String customerEmail,
            String customerName,
            BigDecimal amount,
            String currency,
            OffsetDateTime requestedAt) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EmailSendStatusDto(
            UUID notificationId,
            UUID orderId,
            String customerEmail,
            String status,
            int attemptCount,
            String errorMessage,
            OffsetDateTime updatedAt) {}

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            OffsetDateTime accessTokenExpiresAt,
            UserProfileDto user) {}
}

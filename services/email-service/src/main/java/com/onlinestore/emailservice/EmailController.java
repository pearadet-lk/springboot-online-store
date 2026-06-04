package com.onlinestore.emailservice;

import com.onlinestore.contracts.Dtos.EmailSendStatusDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class EmailController {

    private final EmailStatusStore store;
    private final KafkaMessagingProperties kafka;

    public EmailController(EmailStatusStore store, KafkaMessagingProperties kafka) {
        this.store = store;
        this.kafka = kafka;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "service", "email-service",
                "status", "ok",
                "topic", kafka.topic() != null ? kafka.topic() : "email-notifications");
    }

    @GetMapping("/email/status/{orderId}")
    public EmailSendStatusDto status(@PathVariable UUID orderId) {
        var status = store.getByOrderId(orderId);
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return status;
    }

    @GetMapping("/email/status")
    public List<EmailSendStatusDto> all() {
        return List.copyOf(store.all());
    }
}

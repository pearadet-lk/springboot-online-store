package com.onlinestore.historyservice;

import com.onlinestore.contracts.Dtos.OrderHistoryEventDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
public class HistoryController {

    private final List<OrderHistoryEventDto> events = Collections.synchronizedList(new ArrayList<>());

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "history-service", "status", "ok");
    }

    @PostMapping("/history/events")
    public ResponseEntity<OrderHistoryEventDto> append(@RequestBody OrderHistoryEventDto request) {
        var historyId = request.historyId();
        if (historyId == null || historyId.getLeastSignificantBits() == 0 && historyId.getMostSignificantBits() == 0) {
            historyId = UUID.randomUUID();
        }
        var event = new OrderHistoryEventDto(
                historyId,
                request.orderId(),
                request.userId(),
                request.eventType(),
                request.createdAt() != null ? request.createdAt() : OffsetDateTime.now(ZoneOffset.UTC),
                request.notes());
        events.add(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @GetMapping("/history/users/{userId}")
    public List<OrderHistoryEventDto> listByUser(@PathVariable UUID userId) {
        return events.stream().filter(e -> e.userId().equals(userId)).collect(Collectors.toList());
    }
}

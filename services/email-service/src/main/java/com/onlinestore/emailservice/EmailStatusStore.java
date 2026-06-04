package com.onlinestore.emailservice;

import com.onlinestore.contracts.Dtos.EmailSendStatusDto;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmailStatusStore {

    private final Map<UUID, EmailSendStatusDto> byOrderId = new ConcurrentHashMap<>();
    private final Map<UUID, EmailSendStatusDto> byNotificationId = new ConcurrentHashMap<>();

    public void save(EmailSendStatusDto status) {
        byOrderId.put(status.orderId(), status);
        byNotificationId.put(status.notificationId(), status);
    }

    public EmailSendStatusDto getByOrderId(UUID orderId) {
        return byOrderId.get(orderId);
    }

    public java.util.Collection<EmailSendStatusDto> all() {
        return byOrderId.values();
    }
}

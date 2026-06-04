package com.onlinestore.emailservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinestore.contracts.Dtos.EmailNotificationRequestedEvent;
import com.onlinestore.contracts.Dtos.EmailSendStatusDto;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class EmailNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationListener.class);

    private final ObjectMapper objectMapper;
    private final EmailStatusStore store;
    private final EmailSender emailSender;

    public EmailNotificationListener(ObjectMapper objectMapper, EmailStatusStore store, EmailSender emailSender) {
        this.objectMapper = objectMapper;
        this.store = store;
        this.emailSender = emailSender;
    }

    @KafkaListener(topics = "${messaging.kafka.topic:email-notifications}", groupId = "${messaging.kafka.group-id:email-service}")
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        if (record.value() == null || record.value().isBlank()) {
            ack.acknowledge();
            return;
        }
        EmailNotificationRequestedEvent notification;
        try {
            notification = objectMapper.readValue(record.value(), EmailNotificationRequestedEvent.class);
        } catch (Exception ex) {
            log.warn("Skipping malformed email notification payload.", ex);
            ack.acknowledge();
            return;
        }

        var queued = new EmailSendStatusDto(
                notification.notificationId(),
                notification.orderId(),
                notification.customerEmail(),
                "Queued",
                0,
                null,
                OffsetDateTime.now(ZoneOffset.UTC));
        store.save(queued);

        var processing = new EmailSendStatusDto(
                notification.notificationId(),
                notification.orderId(),
                notification.customerEmail(),
                "Processing",
                1,
                null,
                OffsetDateTime.now(ZoneOffset.UTC));
        store.save(processing);

        try {
            emailSender.send(notification);
            store.save(new EmailSendStatusDto(
                    notification.notificationId(),
                    notification.orderId(),
                    notification.customerEmail(),
                    "Sent",
                    1,
                    null,
                    OffsetDateTime.now(ZoneOffset.UTC)));
            log.info("Email sent for order {}", notification.orderId());
        } catch (Exception ex) {
            store.save(new EmailSendStatusDto(
                    notification.notificationId(),
                    notification.orderId(),
                    notification.customerEmail(),
                    "Failed",
                    1,
                    ex.getMessage(),
                    OffsetDateTime.now(ZoneOffset.UTC)));
            log.error("Email failed for order {}", notification.orderId(), ex);
        }

        ack.acknowledge();
    }
}

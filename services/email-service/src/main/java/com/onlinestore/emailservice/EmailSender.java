package com.onlinestore.emailservice;

import com.onlinestore.contracts.Dtos.EmailNotificationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final EmailSmtpProperties smtp;

    public EmailSender(EmailSmtpProperties smtp) {
        this.smtp = smtp;
    }

    public void send(EmailNotificationRequestedEvent notification) {
        if (smtp.smtpHost() == null || smtp.smtpHost().isBlank()) {
            log.info(
                    "Mock email to {} for order {} amount {} {}",
                    notification.customerEmail(),
                    notification.orderId(),
                    notification.amount(),
                    notification.currency());
            return;
        }
        // Production SMTP wiring can be added here; mock path matches reference when SMTP is unset.
        log.info("SMTP send stub for order {}", notification.orderId());
    }
}

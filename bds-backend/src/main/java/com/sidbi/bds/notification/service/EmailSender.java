package com.sidbi.bds.notification.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {

    public enum Result {
        SENT,
        FAILED,
        SKIPPED
    }

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final boolean enabled;

    public EmailSender(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${bds.mail.from:no-reply@bdsmarketplace.in}") String from,
                       @Value("${bds.mail.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.from = from;
        this.enabled = enabled;
    }

    public Result send(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!enabled || sender == null || to == null || to.isBlank()) {
            return Result.SKIPPED;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body + "\n\n--\nBDS Marketplace\nThis is an automated message. Please do not reply.");
            sender.send(message);
            return Result.SENT;
        } catch (MailException ex) {
            return Result.FAILED;
        }
    }
}

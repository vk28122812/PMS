package com.pm.billingservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    @KafkaListener(topics = "billing-account", groupId = "billing-service")
    public void consumeEvent(byte[] event) {
        try {
            billing.events.BillingAccountEvent billingAccountEvent = billing.events.BillingAccountEvent.parseFrom(event);
            log.info("Received BillingAccountEvent: [PatientId={}, PatientName={}, PatientEmail: {}", billingAccountEvent.getPatientId(), billingAccountEvent.getName(), billingAccountEvent.getEmail());

            // Business logic - for example, save to db, preform calculations, etc.
        } catch (InvalidProtocolBufferException e) {
            log.error("Error parsing BillingAccountEvent {}", e.getMessage());
        }
    }

}

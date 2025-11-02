package com.pm.analyticsservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    // groupId tells Kafka to which consumer group this consumer belongs
    @KafkaListener(topics = "patient", groupId = "analytics-service")
    public void consumeEvent(byte[] event) throws InvalidProtocolBufferException {
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(event);
            // ... process the event (e.g., update analytics database)
            // Suggested Correction:
            log.info("Received patient event: Patient[id={}, name={}, email={}]",
                    patientEvent.getPatientId(), patientEvent.getName(), patientEvent.getEmail());

        } catch (InvalidProtocolBufferException e) {
            log.warn("Error deserializing event: {}", e.getMessage());
        }
    }
}

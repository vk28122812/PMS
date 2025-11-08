package com.pm.patientservice.kafka;

import com.pm.patientservice.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    // telling kafka that the key is a string and the value is a byte array

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(Patient patient) {
        PatientEvent patientEvent = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setEventType("PATIENT_CREATED")
                .build();

        try {

            kafkaTemplate.send("patient", patientEvent.toByteArray());

        } catch (Exception e) {
            log.warn("Error sending PatientCreated event {} to Kafka. Error:  {}", patientEvent, e.getMessage());
        }
    }

    public void sendBillingAccountEvent(String patientId, String name, String email) {
        billing.events.BillingAccountEvent billingAccountEvent = billing.events.BillingAccountEvent.newBuilder()
                .setPatientId(patientId)
                .setName(name)
                .setEmail(email)
                .setEventType("BILLING_ACCOUNT_CREATE_REQUESTED")
                .build();


        try{
            kafkaTemplate.send("billing-account", billingAccountEvent.toByteArray());
        }catch(Exception e){
            log.warn("Error sending BillingAccountEvent {} to Kafka. Error:  {}", billingAccountEvent, e.getMessage());
        }
    }


}

package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;

    public PatientService(PatientRepository patientRepository, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
    }

    public List<PatientResponseDTO> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();

        return patients.stream()
                .map(PatientMapper::toDto).toList();
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientDto) {

        if (patientRepository.existsByEmail(patientDto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + patientDto.getEmail());
        }
        Patient newPatient = patientRepository.save(PatientMapper.toModel(patientDto));

        billingServiceGrpcClient.createBillingAccount(newPatient.getId().toString(), newPatient.getName(), newPatient.getEmail());

        kafkaProducer.sendEvent(newPatient);

        return PatientMapper.toDto(newPatient);
    }

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientDto) {

        Patient patient = patientRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + id));

        if (patientRepository.existsByEmailAndIdNot(patientDto.getEmail(), patient.getId())) {
            throw new EmailAlreadyExistsException("Email already exists: " + patientDto.getEmail());
        }

        patient.setName(patientDto.getName());
        patient.setAddress(patientDto.getAddress());
        patient.setEmail(patientDto.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientDto.getDateOfBirth()));

        Patient updatedPatient = patientRepository.save(patient);
        return PatientMapper.toDto(updatedPatient);
    }

    public void deletePatient(UUID id) {
        patientRepository.deleteById(id);
    }
}

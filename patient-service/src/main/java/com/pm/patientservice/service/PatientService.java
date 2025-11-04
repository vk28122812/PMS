package com.pm.patientservice.service;

import com.pm.patientservice.dto.PagedPatientResponseDTO;
import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;

    public PatientService(PatientRepository patientRepository, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
    }

    @Cacheable(
            value = "patients",
            key = "#page + '-' + #size + '-' + #sort + '-' + #sortField",
            condition = "#searchValue == ''"
    )
    public PagedPatientResponseDTO getAllPatients(int page, int size, String sort, String sortField, String searchValue) {

        log.info("[REDIS] : Cache miss - fetching from DB");

        try{
            Thread.sleep(2000);
        }catch (InterruptedException e){
            log.error("Error: {}", e.getMessage());
        }

        Pageable pageable = PageRequest.of(page, size,
                sort.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending());

        Page<Patient> patientPage ;

        if(searchValue == null || searchValue.isBlank()){
            patientPage = patientRepository.findAll(pageable);
        }else{
            patientPage = patientRepository.findByNameContainingIgnoreCase(searchValue, pageable);
        }
        List<PatientResponseDTO> patientResponseDTOs = patientPage.getContent()
                .stream().map(PatientMapper::toDto).toList();

        return new PagedPatientResponseDTO(
                patientResponseDTOs,
                patientPage.getNumber() + 1, // Spring uses 0-based page numbers
                patientPage.getSize(),
                patientPage.getTotalPages(),
                (int) patientPage.getTotalElements()
        );
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

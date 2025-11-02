package com.pm.patientservice.mapper;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.model.Patient;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;

public class PatientMapper {
    public static PatientResponseDTO toDto(Patient patient) {
        PatientResponseDTO patientDto = new PatientResponseDTO();
        patientDto.setId(patient.getId().toString());
        patientDto.setName(patient.getName());
        patientDto.setEmail(patient.getEmail());
        patientDto.setAddress(patient.getAddress());
        patientDto.setDateOfBirth(patient.getDateOfBirth().toString());
        return patientDto;
    }

    public static Patient toModel(PatientRequestDTO patientRequestDto) {
        Patient patient = new Patient();
        patient.setName(patientRequestDto.getName());
        patient.setAddress(patientRequestDto.getAddress());
        patient.setEmail(patientRequestDto.getEmail());
        patient.setRegisteredDate(LocalDate.parse(patientRequestDto.getRegisteredDate()));
        patient.setDateOfBirth(LocalDate.parse(patientRequestDto.getDateOfBirth()));

        return patient;
    }
}
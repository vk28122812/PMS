package com.pm.appointmentservice.service;

import com.pm.appointmentservice.dto.AppointmentResponseDTO;
import com.pm.appointmentservice.entity.Appointment;
import com.pm.appointmentservice.entity.CachedPatient;
import com.pm.appointmentservice.repository.AppointmentRepository;
import com.pm.appointmentservice.repository.CachedPatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    // CQRS (Command Query Responsibility Segregation) Pattern
    // Separates read operations (queries) from write operations (commands)

    // Read Model : optimized for read operations
    // Write Model : optimized for write operations

    private final AppointmentRepository appointmentRepository;
    private final CachedPatientRepository cachedPatientRepository;

    public AppointmentService(AppointmentRepository appointmentRepository, CachedPatientRepository cachedPatientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.cachedPatientRepository = cachedPatientRepository;
    }

    public List<AppointmentResponseDTO> getAppointmentsByDateRange(LocalDateTime from, LocalDateTime to){
        List<Appointment> appointments = appointmentRepository.findByStartTimeBetween(from, to);
        return appointments.stream().map( appointment -> {

            String name = cachedPatientRepository.findById(appointment.getPatientId())
                    .map(CachedPatient::getFullName)
                    .orElse("Unknown");

            AppointmentResponseDTO appointmentResponseDto = new AppointmentResponseDTO();
            appointmentResponseDto.setStartTime(appointment.getStartTime());
            appointmentResponseDto.setEndTime(appointment.getEndTime());
            appointmentResponseDto.setReason(appointment.getReason());
            appointmentResponseDto.setId(appointment.getId());
            appointmentResponseDto.setVersion(appointment.getVersion());
            appointmentResponseDto.setPatientId(appointment.getPatientId());
            appointmentResponseDto.setPatientName(name);

            return appointmentResponseDto;
        }).toList();
    }


}

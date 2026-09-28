package org.example.appointmentservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointmentservice.models.constants.AppointmentStatus;
import org.example.appointmentservice.models.dto.requests.CreateAppointmentRequest;
import org.example.appointmentservice.models.dto.responses.AppointmentDetailResponse;
import org.example.appointmentservice.models.dto.responses.AppointmentResponse;
import org.example.appointmentservice.models.dto.responses.DoctorResponse;
import org.example.appointmentservice.models.entities.Appointment;
import org.example.appointmentservice.models.entities.AppointmentDetail;
import org.example.appointmentservice.models.repositories.AppointmentDetailRepository;
import org.example.appointmentservice.models.repositories.AppointmentRepository;
import org.example.appointmentservice.models.services.AppointmentService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentDetailRepository appointmentDetailRepository;
    private final DoctorGatewayService doctorGatewayService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        Appointment appointment = Appointment.builder()
                .patientName(request.patientName())
                .patientEmail(request.patientEmail())
                .totalAmount(0.0)
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);

        double totalAmount = 0.0;
        List<AppointmentDetailResponse> detailResponses = new ArrayList<>();

        for (var item : request.items()) {
            DoctorResponse doctor = doctorGatewayService.getDoctorById(item.doctorId());

            if (doctor.availableSlots() == null || doctor.availableSlots() < item.slots()) {
                throw new IllegalArgumentException("Không đủ lượt khám cho bác sĩ ID: " + item.doctorId());
            }

            double lineTotal = doctor.consultationFee() * item.slots();
            totalAmount += lineTotal;

            AppointmentDetail detail = AppointmentDetail.builder()
                    .appointment(savedAppointment)
                    .doctorId(doctor.id())
                    .slots(item.slots())
                    .consultationFee(doctor.consultationFee())
                    .lineTotal(lineTotal)
                    .build();

            AppointmentDetail savedDetail = appointmentDetailRepository.save(detail);

            detailResponses.add(new AppointmentDetailResponse(
                    savedDetail.getId(),
                    savedDetail.getDoctorId(),
                    savedDetail.getSlots(),
                    savedDetail.getConsultationFee(),
                    savedDetail.getLineTotal()
            ));
        }

        savedAppointment.setTotalAmount(totalAmount);
        appointmentRepository.save(savedAppointment);

        try {
            kafkaTemplate.send("appointment-created", request.patientEmail());
        } catch (Exception e) {
            log.error("Failed to send Kafka message for appointment creation: {}", e.getMessage(), e);
        }

        return new AppointmentResponse(
                savedAppointment.getId(),
                savedAppointment.getPatientName(),
                savedAppointment.getPatientEmail(),
                savedAppointment.getTotalAmount(),
                savedAppointment.getStatus(),
                detailResponses
        );
    }
}


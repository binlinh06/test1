package org.example.appointmentservice.models.services;

import org.example.appointmentservice.models.constants.AppointmentStatus;
import org.example.appointmentservice.models.dto.requests.CreateAppointmentDetailRequest;
import org.example.appointmentservice.models.dto.requests.CreateAppointmentRequest;
import org.example.appointmentservice.models.dto.responses.AppointmentResponse;
import org.example.appointmentservice.models.dto.responses.DoctorResponse;
import org.example.appointmentservice.models.entities.Appointment;
import org.example.appointmentservice.models.entities.AppointmentDetail;
import org.example.appointmentservice.models.repositories.AppointmentDetailRepository;
import org.example.appointmentservice.models.repositories.AppointmentRepository;
import org.example.appointmentservice.models.services.impl.AppointmentServiceImpl;
import org.example.appointmentservice.models.services.impl.DoctorGatewayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentDetailRepository appointmentDetailRepository;

    @Mock
    private DoctorGatewayService doctorGatewayService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    @Test
    void createAppointment_Success() {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                "Patient A",
                "patienta@example.com",
                List.of(new CreateAppointmentDetailRequest(1L, 2))
        );

        DoctorResponse doctor = new DoctorResponse(1L, "Dr. Nguyen Van A", "Cardiology", 500000.0, 10);
        when(doctorGatewayService.getDoctorById(1L)).thenReturn(doctor);

        Appointment savedInitial = Appointment.builder()
                .id(10L)
                .patientName(request.patientName())
                .patientEmail(request.patientEmail())
                .totalAmount(0.0)
                .status(AppointmentStatus.PENDING)
                .build();
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedInitial);

        AppointmentDetail savedDetail = AppointmentDetail.builder()
                .id(100L)
                .appointment(savedInitial)
                .doctorId(1L)
                .slots(2)
                .consultationFee(500000.0)
                .lineTotal(1000000.0)
                .build();
        when(appointmentDetailRepository.save(any(AppointmentDetail.class))).thenReturn(savedDetail);

        AppointmentResponse response = appointmentService.createAppointment(request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Patient A", response.patientName());
        assertEquals("patienta@example.com", response.patientEmail());
        assertEquals(1000000.0, response.totalAmount());
        assertEquals(1, response.details().size());
        assertEquals(1000000.0, response.details().get(0).lineTotal());

        verify(appointmentRepository, times(2)).save(any(Appointment.class));
        verify(appointmentDetailRepository, times(1)).save(any(AppointmentDetail.class));
        verify(kafkaTemplate, times(1)).send(eq("appointment-created"), eq("patienta@example.com"));
    }

    @Test
    void createAppointment_InsufficientSlots_ThrowsIllegalArgumentException() {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                "Patient B",
                "patientb@example.com",
                List.of(new CreateAppointmentDetailRequest(2L, 5))
        );

        DoctorResponse doctor = new DoctorResponse(2L, "Dr. Tran Thi B", "Dermatology", 300000.0, 3);
        when(doctorGatewayService.getDoctorById(2L)).thenReturn(doctor);

        Appointment savedInitial = Appointment.builder()
                .id(20L)
                .patientName(request.patientName())
                .patientEmail(request.patientEmail())
                .totalAmount(0.0)
                .status(AppointmentStatus.PENDING)
                .build();
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedInitial);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.createAppointment(request)
        );

        assertTrue(exception.getMessage().contains("Không đủ lượt khám"));
        verify(appointmentDetailRepository, never()).save(any());
    }
}

package org.example.appointmentservice.models.services;

import feign.FeignException;
import org.example.appointmentservice.clients.DoctorClient;
import org.example.appointmentservice.exceptions.DoctorNotFoundException;
import org.example.appointmentservice.exceptions.DoctorServiceException;
import org.example.appointmentservice.models.dto.responses.DoctorResponse;
import org.example.appointmentservice.models.services.impl.DoctorGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorGatewayServiceTest {

    @Mock
    private DoctorClient doctorClient;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private DoctorGatewayService doctorGatewayService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        lenient().when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
        lenient().when(circuitBreaker.run(any(Supplier.class), any(Function.class))).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            Function<Throwable, ?> fallback = invocation.getArgument(1);
            try {
                return supplier.get();
            } catch (Throwable throwable) {
                return fallback.apply(throwable);
            }
        });
    }

    @Test
    void getDoctorById_Success() {
        DoctorResponse expected = new DoctorResponse(1L, "Dr. Nguyen Van A", "Cardiology", 500000.0, 10);
        when(doctorClient.getDoctorById(1L)).thenReturn(expected);

        DoctorResponse actual = doctorGatewayService.getDoctorById(1L);

        assertEquals(expected, actual);
        verify(doctorClient).getDoctorById(1L);
    }

    @Test
    void getDoctorById_NotFound() {
        FeignException.NotFound feignNotFound = mock(FeignException.NotFound.class);
        when(doctorClient.getDoctorById(99L)).thenThrow(feignNotFound);

        assertThrows(DoctorNotFoundException.class, () -> doctorGatewayService.getDoctorById(99L));
    }

    @Test
    void getDoctorById_ServiceUnavailable() {
        when(doctorClient.getDoctorById(1L)).thenThrow(new RuntimeException("Connection refused"));

        assertThrows(DoctorServiceException.class, () -> doctorGatewayService.getDoctorById(1L));
    }
}

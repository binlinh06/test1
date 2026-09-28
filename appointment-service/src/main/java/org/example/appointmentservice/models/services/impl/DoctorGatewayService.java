package org.example.appointmentservice.models.services.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.example.appointmentservice.clients.DoctorClient;
import org.example.appointmentservice.exceptions.DoctorNotFoundException;
import org.example.appointmentservice.exceptions.DoctorServiceException;
import org.example.appointmentservice.models.dto.responses.DoctorResponse;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DoctorGatewayService {
    private final DoctorClient doctorClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public DoctorResponse getDoctorById(Long doctorId) {
        return circuitBreakerFactory.create("appointmentService").run(
                () -> {
                    try {
                        return doctorClient.getDoctorById(doctorId);
                    } catch (FeignException.NotFound e) {
                        throw new DoctorNotFoundException(doctorId);
                    }
                },
                throwable -> {
                    if (throwable instanceof DoctorNotFoundException doctorNotFoundException) {
                        throw doctorNotFoundException;
                    }
                    throw new DoctorServiceException("Doctor service unavailable", throwable);
                }
        );
    }
}


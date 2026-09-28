package org.example.apigateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthHeaderFilterTest {

    private AuthHeaderFilter authHeaderFilter;

    @Mock
    private GatewayFilterChain filterChain;

    @BeforeEach
    void setUp() {
        authHeaderFilter = new AuthHeaderFilter();
    }

    @Test
    void filter_MissingHeader_Returns401Unauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/appointments").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(authHeaderFilter.filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_WithRoleHeader_CallsNextFilter() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/appointments")
                .header("X-User-Role", "PATIENT")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(authHeaderFilter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(exchange);
    }
}

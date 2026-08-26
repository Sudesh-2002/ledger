package com.sudesh.ledger.eventstore;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OutboxCircuitBreakerTest {

    @Autowired private CircuitBreakerRegistry registry;

    @Test
    void circuitBreakerRegisteredWithExpectedConfig() {
        CircuitBreaker cb = registry.circuitBreaker("kafkaPublish");
        assertThat(cb.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(20);
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
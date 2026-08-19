package com.sudesh.ledger.shared.idempotency;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyFilterIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void missingIdempotencyKeyReturns400() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"acc-x\",\"ownerName\":\"Test\",\"openingBalance\":10.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateKeyReturnsCachedResponseNotDoubleProcessed() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = "{\"accountId\":\"" + UUID.randomUUID() + "\",\"ownerName\":\"Test\",\"openingBalance\":10.00}";

        mockMvc.perform(post("/api/accounts")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());

        // retry with the SAME key — should return the cached 202, not fail on "account already exists"
        mockMvc.perform(post("/api/accounts")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());
    }
}
package com.sudesh.ledger.shared.idempotency;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "idempotency_key")
public class IdempotencyKeyEntity {

    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    private String requestPath;
    private String status; // PROCESSING, COMPLETED
    private Integer responseStatus;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    private Instant createdAt = Instant.now();

    protected IdempotencyKeyEntity() {}

    public IdempotencyKeyEntity(String idempotencyKey, String requestPath, String status) {
        this.idempotencyKey = idempotencyKey;
        this.requestPath = requestPath;
        this.status = status;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
}
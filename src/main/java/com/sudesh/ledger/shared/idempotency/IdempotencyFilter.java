package com.sudesh.ledger.shared.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String HEADER = "Idempotency-Key";

    private final IdempotencyService idempotencyService;

    public IdempotencyFilter(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // only guard command (mutating) endpoints — GET/query traffic passes straight through
        return !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing required header: " + HEADER + "\"}");
            return;
        }

        IdempotencyService.ReservationResult result = idempotencyService.reserve(key, request.getRequestURI());

        switch (result.outcome()) {
            case ALREADY_COMPLETED -> {
                // replay the original response verbatim — the retry never touches the command service
                response.setStatus(result.existing().getResponseStatus());
                response.setContentType("application/json");
                response.getWriter().write(result.existing().getResponseBody());
            }
            case IN_PROGRESS -> {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"A request with this idempotency key is already being processed\"}");
            }
            case ACQUIRED -> {
                ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
                try {
                    chain.doFilter(request, wrapped);
                } finally {
                    byte[] body = wrapped.getContentAsByteArray();
                    String bodyStr = new String(body, StandardCharsets.UTF_8);

                    if (wrapped.getStatus() < 500) {
                        idempotencyService.markCompleted(key, wrapped.getStatus(), bodyStr);
                    } else {
                        // don't cache 5xx as a "completed" outcome — let the client legitimately retry
                        idempotencyService.releaseOnFailure(key);
                    }
                    wrapped.copyBodyToResponse(); // must copy back or the real response body is empty
                }
            }
        }
    }
}
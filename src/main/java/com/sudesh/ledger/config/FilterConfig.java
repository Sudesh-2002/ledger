package com.sudesh.ledger.config;

import com.sudesh.ledger.shared.idempotency.IdempotencyFilter;
import com.sudesh.ledger.shared.idempotency.IdempotencyService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilter(IdempotencyService idempotencyService) {
        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new IdempotencyFilter(idempotencyService));
        registration.addUrlPatterns("/api/accounts/*");
        registration.setOrder(1); // run early, before controller/validation logic
        return registration;
    }
}
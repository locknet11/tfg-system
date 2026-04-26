package com.spulido.tfg.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import lombok.Getter;

@Configuration
@Getter
public class NvdApiConfig {

    @Value("${nvd.api.key:}")
    private String apiKey;

    @Value("${nvd.api.base-url:https://services.nvd.nist.gov/rest/json}")
    private String baseUrl;

    @Value("${nvd.api.rate-limit.requests-per-window:5}")
    private int requestsPerWindow;

    @Value("${nvd.api.rate-limit.window-seconds:30}")
    private int windowSeconds;

    @Value("${vulnerability.cache.staleness-days:30}")
    private int stalenessDays;

    @Bean("nvdRestTemplate")
    public RestTemplate nvdRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}

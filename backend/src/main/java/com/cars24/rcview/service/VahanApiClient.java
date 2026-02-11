package com.cars24.rcview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class VahanApiClient {

    private final RestTemplate restTemplate;

    @Value("${vahan.api.base-url:https://api.cuvora.com/car/partner/vehicle/search/v3}")
    private String baseUrl;

    @Value("${vahan.api.api-key:}")
    private String apiKey;

    @Value("${vahan.api.max-age:999}")
    private String maxAge;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Calls Vahan API and returns raw JSON. Returns empty if error or no data.
     */
    public Optional<JsonNode> search(String vehicleNumber) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("VAHAN_API_KEY not set");
            return Optional.empty();
        }
        String url = baseUrl + "?apiTag=RC_PRO&vehicle_num=" + vehicleNumber.trim() + "&maxAge=" + maxAge;
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("error") && !root.get("error").isNull()) {
                return Optional.empty();
            }
            return Optional.of(root);
        } catch (Exception e) {
            log.error("Vahan API call failed for vehicle {}", vehicleNumber, e);
            return Optional.empty();
        }
    }
}

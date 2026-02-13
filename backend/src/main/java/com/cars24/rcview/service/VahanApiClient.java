package com.cars24.rcview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.Optional;

/** Result of a Vahan API call: either data or an error message to show the user. */
class VahanSearchResult {
    private final Optional<JsonNode> data;
    private final String errorMessage;

    private VahanSearchResult(Optional<JsonNode> data, String errorMessage) {
        this.data = data;
        this.errorMessage = errorMessage;
    }

    static VahanSearchResult ok(JsonNode root) {
        return new VahanSearchResult(Optional.of(root), null);
    }

    static VahanSearchResult error(String message) {
        return new VahanSearchResult(Optional.empty(), message);
    }

    Optional<JsonNode> getData() { return data; }
    String getErrorMessage() { return errorMessage; }
}

@Service
public class VahanApiClient {

    private static final Logger log = LoggerFactory.getLogger(VahanApiClient.class);

    private final RestTemplate restTemplate;

    @Value("${vahan.api.base-url:https://api.cuvora.com/car/partner/vehicle/search/v3}")
    private String baseUrl;

    @Value("${vahan.api.api-key:${vahan.api.apiKey:}}")
    private String apiKeyRaw;

    @Value("${vahan.api.max-age:999}")
    private String maxAge;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public VahanApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    private void logKeyStatus() {
        String key = getApiKey();
        if (key.isBlank()) {
            log.warn("Vahan API key is NOT set. Set vahan.api.api-key in application.yml (under vahan.api:) or VAHAN_API_KEY env var, then restart.");
        } else {
            log.info("Vahan API key is set (length={}). Vehicle search will call the Vahan API.", key.length());
        }
    }

    private String getApiKey() {
        return apiKeyRaw != null ? apiKeyRaw.trim() : "";
    }

    /** Returns true if the Vahan API key is set (search will be attempted). */
    public boolean isApiKeyConfigured() {
        return !getApiKey().isBlank();
    }

    /**
     * Calls Vahan API and returns result with data or a user-facing error message.
     */
    public VahanSearchResult search(String vehicleNumber) {
        if (!isApiKeyConfigured()) {
            log.warn("VAHAN_API_KEY not set or empty (check env and restart backend)");
            return VahanSearchResult.error("Vahan API key is not set. Set VAHAN_API_KEY in the same shell before starting the backend, then restart. See RUN.md.");
        }
        String url = baseUrl + "?apiTag=RC_PRO&vehicle_num=" + vehicleNumber.trim() + "&maxAge=" + maxAge;
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", getApiKey());
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            if (response.getBody() == null) {
                return VahanSearchResult.error("Vahan API returned an empty response.");
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("error") && !root.get("error").isNull()) {
                return VahanSearchResult.error("Vahan API returned an error. Check your API key and registration number.");
            }
            return VahanSearchResult.ok(root);
        } catch (HttpStatusCodeException e) {
            int code = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            log.warn("Vahan API HTTP {} for vehicle {}: {}", code, vehicleNumber, body);
            if (code == 401) {
                return VahanSearchResult.error("Invalid Vahan API key (401). Check that VAHAN_API_KEY is correct and restart the backend.");
            }
            if (code == 403) {
                return VahanSearchResult.error("Vahan API access denied (403). Check your API key and permissions.");
            }
            return VahanSearchResult.error("Vahan API error (" + code + "). Try again or check RUN.md.");
        } catch (Exception e) {
            log.error("Vahan API call failed for vehicle {}", vehicleNumber, e);
            return VahanSearchResult.error("Could not reach Vahan API: " + (e.getMessage() != null ? e.getMessage() : "network or server error."));
        }
    }
}

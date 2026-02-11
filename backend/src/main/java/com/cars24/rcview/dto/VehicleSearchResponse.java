package com.cars24.rcview.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSearchResponse {

    private boolean success;
    private boolean fromCache;
    private String registrationNumber;
    private Map<String, Object> data;
    private JsonNode rawResponse;
    private String errorMessage;
}

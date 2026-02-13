package com.cars24.rcview.dto;

import java.util.Map;

public class VehicleSearchResponse {

    private boolean success;
    private boolean fromCache;
    private String registrationNumber;
    private Map<String, Object> data;
    private String errorMessage;

    public VehicleSearchResponse() {
    }

    public VehicleSearchResponse(boolean success, boolean fromCache, String registrationNumber,
                                 Map<String, Object> data, String errorMessage) {
        this.success = success;
        this.fromCache = fromCache;
        this.registrationNumber = registrationNumber;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public boolean isFromCache() { return fromCache; }
    public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public static final class Builder {
        private boolean success;
        private boolean fromCache;
        private String registrationNumber;
        private Map<String, Object> data;
        private String errorMessage;

        public Builder success(boolean success) { this.success = success; return this; }
        public Builder fromCache(boolean fromCache) { this.fromCache = fromCache; return this; }
        public Builder registrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; return this; }
        public Builder data(Map<String, Object> data) { this.data = data; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public VehicleSearchResponse build() {
            return new VehicleSearchResponse(success, fromCache, registrationNumber, data, errorMessage);
        }
    }
}

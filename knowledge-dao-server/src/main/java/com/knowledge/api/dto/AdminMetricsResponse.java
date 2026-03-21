package com.knowledge.api.dto;

import java.util.List;
import java.util.Map;

/** Response DTO for GET /api/admin/metrics */
public class AdminMetricsResponse {
    private long totalRequests;
    private long successRequests;
    private long errorRequests;
    private double avgResponseTimeMs;
    private double p99ResponseTimeMs;
    private Map<String, EndpointMetrics> endpoints;

    public static class EndpointMetrics {
        private long calls;
        private long errors;
        private double avgMs;
        private double p99Ms;

        public long getCalls() { return calls; }
        public void setCalls(long calls) { this.calls = calls; }
        public long getErrors() { return errors; }
        public void setErrors(long errors) { this.errors = errors; }
        public double getAvgMs() { return avgMs; }
        public void setAvgMs(double avgMs) { this.avgMs = avgMs; }
        public double getP99Ms() { return p99Ms; }
        public void setP99Ms(double p99Ms) { this.p99Ms = p99Ms; }
    }

    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
    public long getSuccessRequests() { return successRequests; }
    public void setSuccessRequests(long successRequests) { this.successRequests = successRequests; }
    public long getErrorRequests() { return errorRequests; }
    public void setErrorRequests(long errorRequests) { this.errorRequests = errorRequests; }
    public double getAvgResponseTimeMs() { return avgResponseTimeMs; }
    public void setAvgResponseTimeMs(double avgResponseTimeMs) { this.avgResponseTimeMs = avgResponseTimeMs; }
    public double getP99ResponseTimeMs() { return p99ResponseTimeMs; }
    public void setP99ResponseTimeMs(double p99ResponseTimeMs) { this.p99ResponseTimeMs = p99ResponseTimeMs; }
    public Map<String, EndpointMetrics> getEndpoints() { return endpoints; }
    public void setEndpoints(Map<String, EndpointMetrics> endpoints) { this.endpoints = endpoints; }
}

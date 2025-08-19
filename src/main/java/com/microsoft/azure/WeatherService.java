package com.microsoft.azure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service class for interacting with the National Weather Service API.
 * Provides methods to fetch weather alerts and forecasts.
 */
public class WeatherService {
    
    private static final String NWS_API_BASE = "https://api.weather.gov";
    private static final String USER_AGENT = "weather-app/1.0";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public WeatherService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Makes an asynchronous request to the NWS API with proper error handling.
     * 
     * @param url The URL to request
     * @return CompletableFuture containing the JSON response or null if error
     */
    public CompletableFuture<JsonNode> makeNwsRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/geo+json")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenCompose(response -> {
                if (response.statusCode() == 200) {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(response.body());
                        return CompletableFuture.completedFuture(jsonNode);
                    } catch (Exception e) {
                        return CompletableFuture.completedFuture(null);
                    }
                } else {
                    return CompletableFuture.completedFuture(null);
                }
            })
            .exceptionally(throwable -> null);
    }
    
    /**
     * Formats an alert feature into a readable string.
     * 
     * @param feature The alert feature JSON node
     * @return Formatted alert string
     */
    public String formatAlert(JsonNode feature) {
        JsonNode props = feature.get("properties");
        if (props == null) {
            return "Invalid alert format";
        }
        
        String event = props.has("event") ? props.get("event").asText() : "Unknown";
        String area = props.has("areaDesc") ? props.get("areaDesc").asText() : "Unknown";
        String severity = props.has("severity") ? props.get("severity").asText() : "Unknown";
        String description = props.has("description") ? props.get("description").asText() : "No description available";
        String instruction = props.has("instruction") ? props.get("instruction").asText() : "No specific instructions provided";
        
        return String.format("""
            
            Event: %s
            Area: %s
            Severity: %s
            Description: %s
            Instructions: %s
            """, event, area, severity, description, instruction);
    }
    
    /**
     * Gets weather alerts for a US state.
     * 
     * @param state Two-letter US state code (e.g., "CA", "NY")
     * @return CompletableFuture containing formatted alert information
     */
    public CompletableFuture<String> getAlerts(String state) {
        if (state == null || state.length() != 2) {
            return CompletableFuture.completedFuture("Invalid state code. Please provide a two-letter US state code (e.g., CA, NY).");
        }
        
        String url = NWS_API_BASE + "/alerts/active/area/" + state.toUpperCase();
        
        return makeNwsRequest(url).thenApply(data -> {
            if (data == null || !data.has("features")) {
                return "Unable to fetch alerts or no alerts found.";
            }
            
            JsonNode features = data.get("features");
            if (!features.isArray() || features.size() == 0) {
                return "No active alerts for this state.";
            }
            
            StringBuilder alerts = new StringBuilder();
            for (int i = 0; i < features.size(); i++) {
                if (i > 0) {
                    alerts.append("\n---\n");
                }
                alerts.append(formatAlert(features.get(i)));
            }
            
            return alerts.toString();
        });
    }
    
    /**
     * Gets weather forecast for a location.
     * 
     * @param latitude Latitude of the location (-90 to 90)
     * @param longitude Longitude of the location (-180 to 180)
     * @return CompletableFuture containing formatted forecast information
     */
    public CompletableFuture<String> getForecast(double latitude, double longitude) {
        // Validate coordinates
        if (latitude < -90 || latitude > 90) {
            return CompletableFuture.completedFuture("Invalid latitude. Must be between -90 and 90.");
        }
        if (longitude < -180 || longitude > 180) {
            return CompletableFuture.completedFuture("Invalid longitude. Must be between -180 and 180.");
        }
        
        // First get the forecast grid endpoint
        String pointsUrl = String.format("%s/points/%.4f,%.4f", NWS_API_BASE, latitude, longitude);
        
        return makeNwsRequest(pointsUrl).thenCompose(pointsData -> {
            if (pointsData == null || !pointsData.has("properties")) {
                return CompletableFuture.completedFuture("Unable to fetch forecast data for this location.");
            }
            
            JsonNode properties = pointsData.get("properties");
            if (!properties.has("forecast")) {
                return CompletableFuture.completedFuture("No forecast URL available for this location.");
            }
            
            String forecastUrl = properties.get("forecast").asText();
            
            // Get the forecast data
            return makeNwsRequest(forecastUrl).thenApply(forecastData -> {
                if (forecastData == null || !forecastData.has("properties")) {
                    return "Unable to fetch detailed forecast.";
                }
                
                JsonNode forecastProperties = forecastData.get("properties");
                if (!forecastProperties.has("periods")) {
                    return "No forecast periods available.";
                }
                
                JsonNode periods = forecastProperties.get("periods");
                if (!periods.isArray()) {
                    return "Invalid forecast format.";
                }
                
                // Format the periods into a readable forecast (only show next 5 periods)
                StringBuilder forecasts = new StringBuilder();
                int maxPeriods = Math.min(5, periods.size());
                
                for (int i = 0; i < maxPeriods; i++) {
                    if (i > 0) {
                        forecasts.append("\n---\n");
                    }
                    
                    JsonNode period = periods.get(i);
                    String name = period.has("name") ? period.get("name").asText() : "Unknown";
                    String temperature = period.has("temperature") ? period.get("temperature").asText() : "Unknown";
                    String temperatureUnit = period.has("temperatureUnit") ? period.get("temperatureUnit").asText() : "F";
                    String windSpeed = period.has("windSpeed") ? period.get("windSpeed").asText() : "Unknown";
                    String windDirection = period.has("windDirection") ? period.get("windDirection").asText() : "Unknown";
                    String detailedForecast = period.has("detailedForecast") ? period.get("detailedForecast").asText() : "No detailed forecast available";
                    
                    forecasts.append(String.format("""
                        
                        %s:
                        Temperature: %s°%s
                        Wind: %s %s
                        Forecast: %s
                        """, name, temperature, temperatureUnit, windSpeed, windDirection, detailedForecast));
                }
                
                return forecasts.toString();
            });
        });
    }
    
    /**
     * Closes the HTTP client and releases resources.
     */
    public void close() {
        // HttpClient doesn't need explicit closing in Java 11+
        // Resources are automatically managed
    }
}

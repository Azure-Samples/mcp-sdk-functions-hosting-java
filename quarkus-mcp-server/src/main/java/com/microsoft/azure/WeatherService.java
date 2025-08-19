package com.microsoft.azure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
@Unremovable
public class WeatherService {
    private static final Logger LOG = Logger.getLogger(WeatherService.class);
    private static final String USER_AGENT = "weather-app/1.0";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public String getAlerts(String stateCode) {
        if (stateCode == null || stateCode.length() != 2) {
            return "Invalid state code. Please provide a two-letter US state code (e.g., CA, NY).";
        }
        String url = "https://api.weather.gov/alerts/active/area/" + stateCode.toUpperCase(Locale.ROOT);
        try {
            String body = makeNwsRequest(url);
            if (body == null || body.isEmpty()) {
                return "Unable to fetch alerts or no alerts found.";
            }
            JsonNode root = MAPPER.readTree(body);
            JsonNode features = root.path("features");
            if (!features.isArray() || features.size() == 0) {
                return "No active alerts for this state.";
            }

            StringBuilder alerts = new StringBuilder();
            for (int i = 0; i < features.size(); i++) {
                if (i > 0) alerts.append("\n---\n");
                alerts.append(formatAlert(features.get(i)));
            }
            return alerts.toString();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to fetch alerts for %s", stateCode);
            return "Unable to fetch alerts or no alerts found.";
        }
    }

    public String getForecast(double lat, double lon) {
        if (lat < -90 || lat > 90) {
            return "Invalid latitude. Must be between -90 and 90.";
        }
        if (lon < -180 || lon > 180) {
            return "Invalid longitude. Must be between -180 and 180.";
        }
        try {
            // Limit precision to avoid NWS 301 AdjustPointPrecision redirects
            String latStr = String.format(Locale.ROOT, "%.4f", lat);
            String lonStr = String.format(Locale.ROOT, "%.4f", lon);
            String pointsUrl = String.format(Locale.ROOT, "https://api.weather.gov/points/%s,%s", latStr, lonStr);
            String pointsBody = makeNwsRequest(pointsUrl);
            if (pointsBody == null || pointsBody.isEmpty()) {
                return "Unable to fetch forecast data for this location.";
            }
            JsonNode points = MAPPER.readTree(pointsBody);
            String forecastUrl = points.path("properties").path("forecast").asText("");
            if (forecastUrl.isEmpty()) {
                return "No forecast URL available for this location.";
            }
            String forecastBody = makeNwsRequest(forecastUrl);
            if (forecastBody == null || forecastBody.isEmpty()) {
                return "Unable to fetch detailed forecast.";
            }
            JsonNode forecast = MAPPER.readTree(forecastBody);

            JsonNode periods = forecast.path("properties").path("periods");
            if (!periods.isArray()) {
                return "Invalid forecast format.";
            }

            StringBuilder forecasts = new StringBuilder();
            int max = Math.min(5, periods.size());
            for (int i = 0; i < max; i++) {
                if (i > 0) forecasts.append("\n---\n");
                JsonNode p = periods.get(i);
                String name = p.path("name").asText("Unknown");
                String temperature = p.path("temperature").asText("Unknown");
                String temperatureUnit = p.path("temperatureUnit").asText("F");
                String windSpeed = p.path("windSpeed").asText("Unknown");
                String windDirection = p.path("windDirection").asText("Unknown");
                String detailedForecast = p.path("detailedForecast").asText("No detailed forecast available");
                forecasts.append(String.format("""

                    %s:
                    Temperature: %s°%s
                    Wind: %s %s
                    Forecast: %s
                    """, name, temperature, temperatureUnit, windSpeed, windDirection, detailedForecast));
            }
            return forecasts.toString();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to fetch forecast for %f,%f", lat, lon);
            return "Unable to fetch detailed forecast.";
        }
    }

    private String makeNwsRequest(String url) throws IOException, InterruptedException {
        URI next = URI.create(url);
        for (int redirects = 0; redirects < 5; redirects++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(next)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/geo+json, application/json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int sc = resp.statusCode();
            if (sc >= 200 && sc < 300) {
                return resp.body();
            }
            if (sc == 301 || sc == 302 || sc == 303 || sc == 307 || sc == 308) {
                Optional<String> loc = resp.headers().firstValue("Location");
                if (loc.isPresent()) {
                    next = URI.create(loc.get());
                    continue;
                }
                // Some NWS 301 bodies include a JSON 'location' field
                try {
                    JsonNode body = MAPPER.readTree(resp.body());
                    String locFromBody = body.path("location").asText("");
                    if (!locFromBody.isEmpty()) {
                        next = URI.create(locFromBody);
                        continue;
                    }
                } catch (Exception ignored) {
                }
            }
            throw new IOException("HTTP " + sc + " for " + next + ": " + resp.body());
        }
        throw new IOException("Too many redirects for " + url);
    }

    private String formatAlert(JsonNode feature) {
        JsonNode props = feature.get("properties");
        if (props == null) {
            return "Invalid alert format";
        }
        String event = props.path("event").asText("Unknown");
        String area = props.path("areaDesc").asText("Unknown");
        String severity = props.path("severity").asText("Unknown");
        String description = props.path("description").asText("No description available");
        String instruction = props.path("instruction").asText("No specific instructions provided");
        return String.format("""

            Event: %s
            Area: %s
            Severity: %s
            Description: %s
            Instructions: %s
            """, event, area, severity, description, instruction);
    }
}

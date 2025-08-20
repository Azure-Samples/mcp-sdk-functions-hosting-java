package com.microsoft.azure;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

// @Singleton is implied
public class WeatherTools {

    private static final Logger LOG = Logger.getLogger(WeatherTools.class);

    @Inject
    WeatherService weatherService;

    @Tool(name = "get_alerts", description = "Get active weather alerts for a US state")
    ToolResponse getAlerts(@ToolArg(description = "Two-letter US state code (e.g., CA, NY)") String state) {
        try {
            String result = weatherService.getAlerts(state);
            return ToolResponse.success(result);
        } catch (Exception e) {
            LOG.errorf(e, "Error getting alerts for state: %s", state);
            return ToolResponse.error("Failed to retrieve weather alerts. Please try again later.");
        }
    }

    @Tool(name = "get_forecast", description = "Get weather forecast for a location")
    ToolResponse getForecast(
            @ToolArg(description = "Latitude [-90, 90]") double latitude,
            @ToolArg(description = "Longitude [-180, 180]") double longitude) {
        try {
            String result = weatherService.getForecast(latitude, longitude);
            return ToolResponse.success(result);
        } catch (Exception e) {
            LOG.errorf(e, "Error getting forecast for location: %f, %f", latitude, longitude);
            return ToolResponse.error("Failed to retrieve weather forecast. Please try again later.");
        }
    }
}

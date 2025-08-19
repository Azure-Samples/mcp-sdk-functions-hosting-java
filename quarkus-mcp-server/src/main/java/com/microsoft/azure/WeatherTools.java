package com.microsoft.azure;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import jakarta.inject.Inject;

// @Singleton is implied
public class WeatherTools {

    @Inject
    WeatherService weatherService;

    @Tool(name = "get_alerts", description = "Get active weather alerts for a US state")
    ToolResponse getAlerts(@ToolArg(description = "Two-letter US state code (e.g., CA, NY)") String state) {
        String result = weatherService.getAlerts(state);
        return ToolResponse.success(result);
    }

    @Tool(name = "get_forecast", description = "Get weather forecast for a location")
    ToolResponse getForecast(
            @ToolArg(description = "Latitude [-90, 90]") double latitude,
            @ToolArg(description = "Longitude [-180, 180]") double longitude) {
        String result = weatherService.getForecast(latitude, longitude);
        return ToolResponse.success(result);
    }
}

package com.microsoft.azure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.concurrent.CompletableFuture;
import java.util.List;

/**
 * MCP Weather Server implementation using the official Java MCP SDK.
 * Provides weather alerts and forecast tools via the Model Context Protocol.
 */
public class WeatherServer {
    
    private final WeatherService weatherService;
    private final McpSyncServer mcpServer;
    
    // Constructor for Spring Boot dependency injection
    public WeatherServer(McpStreamableServerTransportProvider transportProvider) {
        this.weatherService = new WeatherService();

        // Configure server capabilities
        McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
            .tools(true)  // Enable tool support
            .logging()    // Enable logging support
            .build();
        
        // Create the MCP server with the provided transport provider
        this.mcpServer = McpServer.sync(transportProvider)
            .serverInfo("weather-server", "1.0.0")
            .capabilities(capabilities)
            .tools(createGetAlertsTool(), createGetForecastTool())
            .build();
            
        System.err.println("MCP Weather Server initialized with HTTP Streamable transport");
        System.err.println("Available tools: get_alerts, get_forecast");
    }
    
    // Default constructor for backward compatibility (standalone usage)
    public WeatherServer() {
        this(HttpServletStreamableServerTransportProvider.builder()
                .objectMapper(new ObjectMapper())
                .mcpEndpoint("/mcp")
                .build());
    }
    
    /**
     * Creates the get_alerts tool specification.
     */
    private McpServerFeatures.SyncToolSpecification createGetAlertsTool() {
        var getAlertsSchema = """
            {
                "type": "object",
                "properties": {
                    "state": {
                        "type": "string",
                        "description": "Two-letter US state code (e.g., CA, NY)"
                    }
                },
                "required": ["state"]
            }
            """;
            
        McpSchema.Tool tool = McpSchema.Tool.builder()
            .name("get_alerts")
            .description("Get active weather alerts for a US state")
            .inputSchema(getAlertsSchema)
            .build();
            
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(tool)
            .callHandler((exchange, request) -> {
                try {
                    var arguments = request.arguments();
                    String state = (String) arguments.get("state");
                    if (state == null) {
                        return new McpSchema.CallToolResult(
                            List.of(new McpSchema.TextContent("State parameter is required")), true);
                    }
                    
                    // Log the tool execution
                    exchange.loggingNotification(
                        McpSchema.LoggingMessageNotification.builder()
                            .level(McpSchema.LoggingLevel.INFO)
                            .logger("weather-server")
                            .data("Fetching weather alerts for state: " + state)
                            .build()
                    );
                    
                    CompletableFuture<String> resultFuture = weatherService.getAlerts(state);
                    String result = resultFuture.join(); // Block and wait for result
                    
                    return new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent(result)), false);
                } catch (Exception e) {
                    exchange.loggingNotification(
                        McpSchema.LoggingMessageNotification.builder()
                            .level(McpSchema.LoggingLevel.ERROR)
                            .logger("weather-server")
                            .data("Error fetching alerts: " + e.getMessage())
                            .build()
                    );
                    return new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent("Error fetching weather alerts: " + e.getMessage())), true);
                }
            })
            .build();
    }
    
    /**
     * Creates the get_forecast tool specification.
     */
    private McpServerFeatures.SyncToolSpecification createGetForecastTool() {
        var getForecastSchema = """
            {
                "type": "object",
                "properties": {
                    "latitude": {
                        "type": "number",
                        "description": "Latitude of the location (-90 to 90)",
                        "minimum": -90,
                        "maximum": 90
                    },
                    "longitude": {
                        "type": "number",
                        "description": "Longitude of the location (-180 to 180)",
                        "minimum": -180,
                        "maximum": 180
                    }
                },
                "required": ["latitude", "longitude"]
            }
            """;
            
        McpSchema.Tool tool = McpSchema.Tool.builder()
            .name("get_forecast")
            .description("Get weather forecast for a location")
            .inputSchema(getForecastSchema)
            .build();
            
        return McpServerFeatures.SyncToolSpecification.builder()
            .tool(tool)
            .callHandler((exchange, request) -> {
                try {
                    var arguments = request.arguments();
                    Object latObj = arguments.get("latitude");
                    Object lonObj = arguments.get("longitude");
                    
                    if (latObj == null || lonObj == null) {
                        return new McpSchema.CallToolResult(
                            List.of(new McpSchema.TextContent("Both latitude and longitude parameters are required")), true);
                    }
                    
                    double latitude = convertToDouble(latObj);
                    double longitude = convertToDouble(lonObj);
                    
                    // Log the tool execution
                    exchange.loggingNotification(
                        McpSchema.LoggingMessageNotification.builder()
                            .level(McpSchema.LoggingLevel.INFO)
                            .logger("weather-server")
                            .data("Fetching weather forecast for coordinates: " + latitude + ", " + longitude)
                            .build()
                    );
                    
                    CompletableFuture<String> resultFuture = weatherService.getForecast(latitude, longitude);
                    String result = resultFuture.join(); // Block and wait for result
                    
                    return new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent(result)), false);
                } catch (Exception e) {
                    exchange.loggingNotification(
                        McpSchema.LoggingMessageNotification.builder()
                            .level(McpSchema.LoggingLevel.ERROR)
                            .logger("weather-server")
                            .data("Error fetching forecast: " + e.getMessage())
                            .build()
                    );
                    return new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent("Error fetching weather forecast: " + e.getMessage())), true);
                }
            })
            .build();
    }
    
    /**
     * Converts various number types to double.
     */
    private double convertToDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        } else if (obj instanceof String) {
            return Double.parseDouble((String) obj);
        } else {
            throw new IllegalArgumentException("Cannot convert to double: " + obj);
        }
    }
    
    /**
     * Stops the server and cleans up resources.
     */
    public void stop() {
        try {
            mcpServer.close();
            weatherService.close();
            System.err.println("Weather server stopped.");
        } catch (Exception e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }
    
    /**
     * Main method to run the weather server in standalone mode.
     * For Spring Boot mode, use WeatherServerApplication instead.
     */
    public static void main(String[] args) {
        System.err.println("Starting MCP Weather Server in standalone mode...");
        System.err.println("Note: For HTTP server functionality, use WeatherServerApplication instead.");
        
        WeatherServer server = new WeatherServer();
        
        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shutting down weather server...");
            server.stop();
        }));
        
        try {
            System.err.println("Server initialized. Note: HTTP endpoints require servlet container.");
            // Keep the process alive
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Failed to start weather server: " + e.getMessage());
            System.exit(1);
        }
    }
}

# MCP Weather Server - Java Implementation

A Java implementation of an MCP (Model Context Protocol) Weather Server using the official Java MCP SDK and Spring Boot. This server provides weather alerts and forecasts through the Model Context Protocol, making it compatible with VS Code and other MCP clients.

## 🌟 Features

This MCP weather server provides two tools accessible through the Model Context Protocol:

1. **get_alerts(state)**: Get active weather alerts for a US state
   - Parameters: `state` (string) - Two-letter US state code (e.g., "CA", "NY")
   - Returns: Formatted weather alert information including event type, area, severity, description, and instructions

2. **get_forecast(latitude, longitude)**: Get weather forecast for a location
   - Parameters:
     - `latitude` (double) - Latitude of the location (-90 to 90)
     - `longitude` (double) - Longitude of the location (-180 to 180)
   - Returns: Weather forecast with temperature, wind, and detailed conditions

## 🏗️ Implementation

### Core Components

- **WeatherServer.java**: MCP server implementation using the official Java MCP SDK
- **WeatherService.java**: Weather data service with National Weather Service API integration
- **WeatherServerApplication.java**: Spring Boot application with HTTP SSE transport
- **application.yml**: Logging configuration
- **.vscode/mcp.json**: VS Code MCP client configuration

### Dependencies

- Java 17
- Spring Boot 3.2.1
- Jackson Data bind for JSON processing
- MCP Java SDK (v0.11.0)

## 🚀 Usage

### Running the Server

```bash
# Run with Spring Boot (recommended for development)
mvn spring-boot:run

# Alternative using exec plugin
mvn exec:java

# Build and run as executable jar
mvn clean package -DskipTests
java -jar target/mcp-1.0.jar
```

### VS Code MCP Client Configuration

The `.vscode/mcp.json` file configures VS Code to connect to the weather server:

```json
{
	"servers": {
		"weather-server": {
			"url": "http://localhost:8080/mcp",
			"type": "http"
		}
	},
	"inputs": []
}
```

### Server Endpoints

- **SSE Endpoint**: `http://localhost:8080/mcp`
- **Message Endpoint**: `http://localhost:8080/message`

### Using the Tools

Once connected through VS Code MCP client:

```text
# Get weather alerts for a state
get_alerts(state="CA")

# Get weather forecast for coordinates
get_forecast(latitude=37.7749, longitude=-122.4194)  # San Francisco
```

## 🛡️ National Weather Service API

This implementation uses the official NWS API:

- Base URL: `https://api.weather.gov`
- Alerts endpoint: `/alerts/active/area/{state}`
- Points endpoint: `/points/{lat},{lon}`
- Forecast endpoint: Retrieved from points response

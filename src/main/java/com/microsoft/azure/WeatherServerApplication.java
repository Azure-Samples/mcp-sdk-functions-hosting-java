package com.microsoft.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WeatherServerApplication {

    @Bean
    public HttpServletStreamableServerTransportProvider httpServletStreamableServerTransportProvider() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Ensure null map values are not serialized either:
        mapper.configOverride(Map.class)
                .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));

        return HttpServletStreamableServerTransportProvider.builder()
                .objectMapper(mapper)
                .mcpEndpoint("/mcp")
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletStreamableServerTransportProvider> mcpServletRegistration(
            HttpServletStreamableServerTransportProvider transportProvider) {
        ServletRegistrationBean<HttpServletStreamableServerTransportProvider> registration = 
            new ServletRegistrationBean<>(transportProvider);
        registration.addUrlMappings("/mcp");
        return registration;
    }

    @Bean
    public WeatherServer weatherServer(HttpServletStreamableServerTransportProvider transportProvider) {
        return new WeatherServer(transportProvider);
    }

    public static void main(String[] args) {
        // Read the port from environment variables - MCP_SERVER_PORT has priority
        String port = System.getenv("MCP_SERVER_PORT");
        if (port == null) {
            port = "8080"; // default
        }
        
        System.err.println("Starting MCP Weather Server with Spring Boot...");
        System.err.println("Server will be available at: http://localhost:" + port);
        System.err.println("MCP endpoints:");
        System.err.println("  - MCP: http://localhost:" + port + "/mcp");
        System.err.println("Transport: HTTP Streamable (enhanced session management)");
        System.err.println("Port configuration:");
        System.err.println("  - MCP_SERVER_PORT: " + System.getenv("MCP_SERVER_PORT"));
        System.err.println("  - FUNCTIONS_CUSTOMHANDLER_PORT: " + System.getenv("FUNCTIONS_CUSTOMHANDLER_PORT"));
        System.err.println("  - Using port: " + port);
        SpringApplication.run(WeatherServerApplication.class, args);
    }
}

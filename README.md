# MCP on Azure Functions (Java + Quarkus)

Minimal sample that hosts a Model Context Protocol (MCP) server built with Java/Quarkus on Azure Functions (custom handler). Includes a weather tool and a tiny e2e test.

## What’s here

- `quarkus-mcp-server/`: Quarkus MCP server packaged as an uber‑jar; Azure Functions custom handler wiring
- `e2e/`: Minimal Node-based end‑to‑end test runner

## Prereqs

- Java 17, Maven 3.8+
- Azure Functions Core Tools v4
- Node.js 18+ (for e2e)

## Run locally

```powershell
# Build (production)
cd quarkus-mcp-server
mvn clean package

# OR: Build with debug profile (adds JDWP on 5005)
mvn clean package -P debug

# Start Functions host
cd target/azure-function
func host start
```

Endpoint:
- [http://localhost:7071/mcp](http://localhost:7071/mcp) (server)

## Quick call (PowerShell)

```powershell
$body = @{
  jsonrpc = "2.0"; id = 1; method = "tools/call";
  params = @{ name = "get_alerts"; arguments = @{ state = "CA" } }
} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Post -Uri http://localhost:7071/mcp -ContentType 'application/json' -Body $body
```

## E2E test

```powershell
cd e2e
npm install
npm test
```

## Deploy

```powershell
# Build (production)
cd quarkus-mcp-server
mvn clean package

# Publish to an existing Function App
cd target/azure-function
func azure functionapp publish <your-function-app-name>
```

## Notes

- HTTP binds to 0.0.0.0 and uses FUNCTIONS_CUSTOMHANDLER_PORT
- host*.json templates resolve the runner JAR name at package time

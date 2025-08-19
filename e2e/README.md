# E2E MCP Client Tests

This folder contains a minimal Node-based end-to-end test harness that connects to your MCP servers via HTTP using the official TypeScript SDK.

- No changes to Java modules are required.
- Targets are provided via environment variable, and each target is tested for:
  - Initialization/connect
  - tools/list includes `get_alerts` and `get_forecast`
  - tools/call for both tools returns non-empty text with basic format hints

## Prerequisites

- Node.js 18+

## Install

```powershell
cd e2e
npm install
```

## Run

By default it targets `http://127.0.0.1:8080/mcp`.

```powershell
# Single server
$env:E2E_TARGETS="http://127.0.0.1:8080/mcp"; npm test

# Multiple servers (comma-separated)
$env:E2E_TARGETS="http://127.0.0.1:8080/mcp,http://127.0.0.1:8081/mcp"; npm test
```

If your servers expose the legacy SSE transport at the same path, the harness automatically falls back to SSE when Streamable HTTP fails.

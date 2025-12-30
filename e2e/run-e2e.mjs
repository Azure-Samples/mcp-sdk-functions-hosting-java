import assert from 'node:assert/strict';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';
import { SSEClientTransport } from '@modelcontextprotocol/sdk/client/sse.js';

/**
 * E2E test runner for MCP servers using the official TypeScript SDK.
 *
 * Environment variables:
 *  - E2E_TARGETS: comma-separated list of base URLs (e.g., http://127.0.0.1:8080/mcp)
 *    If not provided, defaults to: http://127.0.0.1:8080/mcp
 */

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function connectClient(url) {
  const base = new URL(url);
  const client = new Client({ name: 'e2e-client', version: '1.0.0' });

  // Prefer Streamable HTTP with a few retries.
  for (let i = 0; i < 3; i++) {
    try {
      const transport = new StreamableHTTPClientTransport(base);
      await client.connect(transport);
      console.log(`[ok] Connected (streamable) -> ${url}`);
      return client;
    } catch (err) {
      if (i === 2) break;
      await sleep(250);
    }
  }

  // Fall back to SSE; try base and base + '/sse'.
  const sseCandidates = [base, new URL(base.pathname.endsWith('/sse') ? base.href : base.href.replace(/\/?$/, '/sse'), base)];
  for (const cand of sseCandidates) {
    try {
      const sseTransport = new SSEClientTransport(cand);
      await client.connect(sseTransport);
      console.log(`[ok] Connected (sse) -> ${cand.href}`);
      return client;
    } catch (err) {
      // try next candidate
      await sleep(200);
    }
  }

  throw new Error(`Unable to connect to ${url} via Streamable HTTP or SSE`);
}

async function listTools(client) {
  const tools = await client.listTools();
  assert.ok(Array.isArray(tools?.tools), 'tools list must be an array');
  console.log(`[ok] tools/list -> ${tools.tools.map(t => t.name).join(', ')}`);
  return tools.tools.map(t => t.name);
}

function pickArgs(name) {
  switch (name) {
    case 'get_alerts':
      return { state: 'CA' };
    case 'get_forecast':
      return { latitude: 37.7749, longitude: -122.4194 };
    default:
      return {};
  }
}

function extractText(result) {
  const items = result?.content ?? [];
  const firstText = items.find(i => i?.type === 'text');
  return firstText?.text ?? '';
}

async function callTool(client, name) {
  const args = pickArgs(name);
  const result = await client.callTool({ name, arguments: args });
  const text = extractText(result);
  assert.ok(typeof text === 'string' && text.length > 0, `tool ${name} returned empty text`);
  console.log(`[ok] tools/call ${name} -> ${(text.slice(0, 80) + (text.length > 80 ? '…' : ''))}`);
  return text;
}

async function runOnce(baseUrl) {
  const client = await connectClient(baseUrl);
  try {
    const names = await listTools(client);

    // Must include our two canonical tools; tolerate extra tools if present.
    assert.ok(names.includes('get_alerts'), 'get_alerts not listed');
    assert.ok(names.includes('get_forecast'), 'get_forecast not listed');

    // Call each expected tool and ensure we get non-empty text back.
    const alerts = await callTool(client, 'get_alerts');
    assert.ok(/event:|no active alerts|unable to fetch/i.test(alerts));

    const forecast = await callTool(client, 'get_forecast');
    assert.ok(/temperature:|unable to fetch/i.test(forecast));
  } finally {
    await sleep(50);
    await client.close();
  }
}

async function main() {
  const rawTargets = process.env.E2E_TARGETS ?? 'http://127.0.0.1:8080/mcp';
  const targets = rawTargets.split(',').map(s => s.trim()).filter(Boolean);
  console.log(`[info] E2E targets ->\n  ${targets.join('\n  ')}`);

  for (const t of targets) {
    console.log(`\n=== Testing ${t} ===`);
    await runOnce(t);
  }
  console.log('\nAll E2E checks passed.');
}

main().catch(err => {
  console.error('\nE2E failed:', err);
  process.exit(1);
});

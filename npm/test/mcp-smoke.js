#!/usr/bin/env node

const assert = require("assert");
const path = require("path");
const readline = require("readline");
const { spawn } = require("child_process");

const jarPath = process.argv[2] && path.resolve(process.argv[2]);
if (!jarPath) {
  console.error("Usage: node test/mcp-smoke.js <server.jar>");
  process.exit(2);
}

const child = spawn("java", ["-Dfile.encoding=UTF-8", "-jar", jarPath], {
  cwd: path.dirname(jarPath),
  stdio: ["pipe", "pipe", "pipe"],
});

const pending = new Map();
let nextId = 1;
let stderr = "";
let settled = false;

child.stderr.setEncoding("utf8");
child.stderr.on("data", (chunk) => { stderr += chunk; });

function send(method, params) {
  const id = nextId++;
  child.stdin.write(`${JSON.stringify({ jsonrpc: "2.0", id, method, params })}\n`);
  return new Promise((resolve, reject) => pending.set(id, { resolve, reject }));
}

function notify(method, params = {}) {
  child.stdin.write(`${JSON.stringify({ jsonrpc: "2.0", method, params })}\n`);
}

const output = readline.createInterface({ input: child.stdout });
output.on("line", (line) => {
  if (!line.trim()) return;
  let message;
  try {
    message = JSON.parse(line);
  } catch (error) {
    finish(error);
    return;
  }
  if (message.id !== undefined && pending.has(message.id)) {
    const handler = pending.get(message.id);
    pending.delete(message.id);
    if (message.error) handler.reject(new Error(JSON.stringify(message.error)));
    else handler.resolve(message.result);
  }
});

child.on("error", finish);
child.on("exit", (code, signal) => {
  if (!settled) finish(new Error(`MCP server exited early: code=${code}, signal=${signal}\n${stderr}`));
});

const timeout = setTimeout(() => finish(new Error(`MCP smoke test timed out\n${stderr}`)), 30_000);

function finish(error) {
  if (settled) return;
  settled = true;
  clearTimeout(timeout);
  output.close();
  child.stdin.destroy();
  child.kill();
  if (error) {
    console.error(error.stack || error);
    process.exitCode = 1;
  }
}

(async () => {
  try {
    const initialized = await send("initialize", {
      protocolVersion: "2024-11-05",
      capabilities: {},
      clientInfo: { name: "commerce-context-smoke", version: "1.0.0" },
    });
    assert.ok(initialized.serverInfo && initialized.serverInfo.name);
    notify("notifications/initialized");

    const [toolResult, resourceResult, templateResult, promptResult, callResult, ruleResult, catalogResult, noAnswerResult] = await Promise.all([
      send("tools/list", {}),
      send("resources/list", {}),
      send("resources/templates/list", {}),
      send("prompts/list", {}),
      send("tools/call", {
        name: "search_knowledge",
        arguments: { query: "결제 웹훅 서명", topK: 3, detail: "summary" },
      }),
      send("tools/call", {
        name: "get_rule",
        arguments: { ruleId: "available-stock-query" },
      }),
      send("resources/read", { uri: "commerce://catalog" }),
      send("tools/call", {
        name: "search_knowledge",
        arguments: { query: "오늘 서울 날씨", topK: 3, detail: "summary" },
      }),
    ]);

    const toolNames = toolResult.tools.map((tool) => tool.name);
    assert.strictEqual(toolNames.length, 4);
    assert.ok(toolNames.includes("search_knowledge"));
    assert.ok(toolNames.includes("get_rule"));
    assert.ok(toolNames.includes("get_checklist"));
    assert.ok(toolNames.includes("review_commerce_design"));

    assert.ok(resourceResult.resources.some((resource) => resource.uri === "commerce://catalog"));
    assert.ok(templateResult.resourceTemplates.some((resource) => resource.uriTemplate === "commerce://rules/{ruleId}"));
    assert.ok(templateResult.resourceTemplates.some((resource) => resource.uriTemplate === "commerce://domains/{domain}"));
    assert.ok(promptResult.prompts.some((prompt) => prompt.name === "review-commerce-api"));
    assert.ok(promptResult.prompts.some((prompt) => prompt.name === "review-payment-webhook"));
    assert.strictEqual(callResult.isError, false);
    assert.ok(
      callResult.content.some((item) => item.type === "text"
        && item.text.includes("webhook-handling")
        && item.text.includes("project-guidance")
        && item.text.includes("matchConfidence")
        && item.text.includes("commerce-context-maintainers")
        && item.text.includes("notice")),
      JSON.stringify(callResult),
    );
    assert.ok(ruleResult.content.some((item) => item.type === "text"
      && item.text.includes('"evidenceLevel":"project-guidance"')
      && item.text.includes('"owner":"commerce-context-maintainers"')
      && item.text.includes('"verifiedBy":null')
      && item.text.includes("not-recorded")));
    assert.ok(catalogResult.contents.some((item) => item.text.includes("project-guidance")
      && item.text.includes("needs-review")));
    assert.ok(noAnswerResult.content.some((item) => item.type === "text"
      && item.text.includes('"answerable":false')
      && item.text.includes("구체화")));

    console.log("MCP JSON-RPC smoke test passed: initialize, discovery, trust metadata, search, and no-answer");
    finish();
  } catch (error) {
    finish(error);
  }
})();

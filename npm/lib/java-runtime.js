const { spawnSync } = require("child_process");

function parseJavaMajor(output) {
  const text = String(output || "");
  const match = text.match(/version "([^"]+)"/) || text.match(/openjdk ([^\s]+)/i);
  if (!match) {
    return null;
  }
  const version = match[1];
  const major = version.startsWith("1.") ? version.split(".")[1] : version.split(".")[0];
  const parsed = Number(major);
  return Number.isFinite(parsed) ? parsed : null;
}

function javaStatus(spawn = spawnSync) {
  const result = spawn("java", ["-version"], { encoding: "utf8" });
  const output = `${result.stdout || ""}${result.stderr || ""}`;
  const major = parseJavaMajor(output);
  return {
    ok: result.status === 0 && major !== null && major >= 17,
    major,
    output: output.trim(),
  };
}

function assertJava() {
  const status = javaStatus();
  if (!status.ok) {
    const suffix = status.output ? `\n${status.output}` : "";
    throw new Error(`Java 17 or newer is required.${suffix}`);
  }
  return status;
}

module.exports = { parseJavaMajor, javaStatus, assertJava };

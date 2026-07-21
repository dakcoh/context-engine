const crypto = require("crypto");
const fs = require("fs");

function parseChecksum(text) {
  const match = String(text || "").trim().match(/(?:^|\s)([a-fA-F0-9]{64})(?:\s|$)/);
  if (!match) {
    throw new Error("Malformed SHA-256 checksum file");
  }
  return match[1].toLowerCase();
}

function sha256OfFile(filePath) {
  return crypto.createHash("sha256").update(fs.readFileSync(filePath)).digest("hex");
}

function verifyFile(filePath, expected) {
  const actual = sha256OfFile(filePath).toLowerCase();
  if (actual !== expected.toLowerCase()) {
    throw new Error(`Checksum mismatch: expected ${expected}, actual ${actual}`);
  }
  return actual;
}

module.exports = { parseChecksum, sha256OfFile, verifyFile };

const fs = require("fs");
const https = require("https");

const DEFAULT_TIMEOUT_MS = 15_000;
const MAX_REDIRECTS = 5;
const ALLOWED_HOSTS = new Set([
  "github.com",
  "objects.githubusercontent.com",
  "release-assets.githubusercontent.com",
]);

function releaseInfo(pkg, repo = "dakcoh/commerce-context-mcp") {
  const jarName = `context-engine-${pkg.version}.jar`;
  const releaseUrl = `https://github.com/${repo}/releases/download/v${pkg.version}/${jarName}`;
  return { jarName, releaseUrl, checksumUrl: `${releaseUrl}.sha256` };
}

function request(url, { timeoutMs = DEFAULT_TIMEOUT_MS, redirects = 0 } = {}) {
  const parsed = new URL(url);
  if (parsed.protocol !== "https:" || !ALLOWED_HOSTS.has(parsed.hostname)) {
    return Promise.reject(new Error(`Refusing untrusted release URL: ${url}`));
  }
  if (redirects > MAX_REDIRECTS) {
    return Promise.reject(new Error(`Too many redirects for ${url}`));
  }

  return new Promise((resolve, reject) => {
    const req = https.get(url, { headers: { "User-Agent": "commerce-context-mcp" } }, (response) => {
      if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
        response.resume();
        const redirected = new URL(response.headers.location, url).toString();
        request(redirected, { timeoutMs, redirects: redirects + 1 }).then(resolve, reject);
        return;
      }
      resolve(response);
    });
    req.setTimeout(timeoutMs, () => req.destroy(new Error(`Request timed out after ${timeoutMs}ms: ${url}`)));
    req.on("error", reject);
  });
}

async function fetchText(url) {
  const response = await request(url);
  if (response.statusCode !== 200) {
    response.resume();
    throw new Error(`Release checksum is not available. HTTP ${response.statusCode}`);
  }
  return new Promise((resolve, reject) => {
    let body = "";
    response.setEncoding("utf8");
    response.on("data", (chunk) => { body += chunk; });
    response.on("end", () => resolve(body));
    response.on("error", reject);
  });
}

async function download(url, destination, onProgress = () => {}) {
  const response = await request(url);
  if (response.statusCode !== 200) {
    response.resume();
    throw new Error(`Release asset is not available. HTTP ${response.statusCode}`);
  }
  const expected = Number(response.headers["content-length"] || 0);
  let received = 0;
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(destination, { flags: "wx" });
    response.on("data", (chunk) => {
      received += chunk.length;
      onProgress(received, expected);
    });
    response.on("error", (error) => {
      file.destroy();
      reject(error);
    });
    file.on("error", reject);
    file.on("finish", () => file.close(() => {
      if (expected > 0 && received !== expected) {
        reject(new Error(`Incomplete download: received ${received} of ${expected} bytes`));
        return;
      }
      resolve({ received, expected });
    }));
    response.pipe(file);
  });
}

module.exports = { releaseInfo, request, fetchText, download, ALLOWED_HOSTS };

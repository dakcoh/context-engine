const fs = require("fs");
const os = require("os");
const path = require("path");
const { download, fetchText } = require("./release-client");
const { parseChecksum, verifyFile } = require("./checksum");

const MAX_ATTEMPTS = 3;
const LOCK_WAIT_MS = 30_000;
const STALE_LOCK_MS = 120_000;

function cachePaths(info, cacheDir = path.join(os.homedir(), ".commerce-context-mcp")) {
  const jarPath = path.join(cacheDir, info.jarName);
  return {
    cacheDir,
    jarPath,
    checksumPath: `${jarPath}.sha256`,
    lockPath: `${jarPath}.lock`,
  };
}

function verifyCached(paths) {
  if (!fs.existsSync(paths.jarPath) || !fs.existsSync(paths.checksumPath)) {
    return { ok: false, reason: "missing" };
  }
  try {
    const expected = parseChecksum(fs.readFileSync(paths.checksumPath, "utf8"));
    const actual = verifyFile(paths.jarPath, expected);
    return { ok: true, actual };
  } catch (error) {
    return { ok: false, reason: error.message };
  }
}

async function ensureJar(info, options = {}) {
  const paths = cachePaths(info, options.cacheDir);
  fs.mkdirSync(paths.cacheDir, { recursive: true });
  const cached = verifyCached(paths);
  if (cached.ok) {
    console.error(`Using verified cached JAR: ${paths.jarPath}`);
    return paths.jarPath;
  }

  const release = await acquireLock(paths);
  if (!release) {
    const completed = verifyCached(paths);
    if (!completed.ok) {
      throw new Error(`Concurrent download did not produce a valid cache: ${completed.reason}`);
    }
    return paths.jarPath;
  }

  try {
    const rechecked = verifyCached(paths);
    if (rechecked.ok) {
      return paths.jarPath;
    }
    await downloadAndVerify(info, paths, options);
    return paths.jarPath;
  } finally {
    release();
  }
}

async function downloadAndVerify(info, paths, options) {
  const fetchChecksum = options.fetchText || fetchText;
  const downloadFile = options.download || download;
  const checksumText = await fetchChecksum(info.checksumUrl);
  const expected = parseChecksum(checksumText);
  let lastError;
  for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt += 1) {
    const tempPath = `${paths.jarPath}.${process.pid}.${Date.now()}.${attempt}.tmp`;
    const checksumTempPath = `${paths.checksumPath}.${process.pid}.${Date.now()}.${attempt}.tmp`;
    try {
      console.error(`Downloading ${info.releaseUrl} (attempt ${attempt}/${MAX_ATTEMPTS})`);
      await downloadFile(info.releaseUrl, tempPath, options.onProgress);
      const actual = verifyFile(tempPath, expected);
      replaceFile(tempPath, paths.jarPath);
      fs.writeFileSync(checksumTempPath, `${actual}  ${info.jarName}\n`, { encoding: "utf8" });
      replaceFile(checksumTempPath, paths.checksumPath);
      console.error(`Cached verified JAR: ${paths.jarPath}`);
      return;
    } catch (error) {
      lastError = error;
      fs.rmSync(tempPath, { force: true });
      fs.rmSync(checksumTempPath, { force: true });
      if (attempt < MAX_ATTEMPTS) {
        console.error(`Download attempt ${attempt} failed: ${error.message}. Retrying...`);
      }
    }
  }
  throw new Error(`Failed to download ${info.jarName}: ${lastError && lastError.message}`);
}

function replaceFile(source, destination) {
  try {
    fs.renameSync(source, destination);
  } catch (error) {
    if (!fs.existsSync(destination) || !["EEXIST", "EPERM", "EACCES"].includes(error.code)) {
      throw error;
    }
    // Windows does not consistently replace an existing destination with rename.
    // The destination is already known to be missing or invalid at this point.
    fs.rmSync(destination, { force: true });
    fs.renameSync(source, destination);
  }
}

async function acquireLock(paths) {
  const started = Date.now();
  while (Date.now() - started < LOCK_WAIT_MS) {
    try {
      const descriptor = fs.openSync(paths.lockPath, "wx");
      fs.writeFileSync(descriptor, `${process.pid}\n`);
      return () => {
        fs.closeSync(descriptor);
        fs.rmSync(paths.lockPath, { force: true });
      };
    } catch (error) {
      if (error.code !== "EEXIST") {
        throw error;
      }
      let stat;
      try {
        stat = fs.statSync(paths.lockPath);
      } catch (statError) {
        // The lock owner may finish between openSync() and statSync(). Retry
        // instead of treating that normal race as a download failure.
        if (statError.code === "ENOENT") {
          continue;
        }
        throw statError;
      }
      if (Date.now() - stat.mtimeMs > STALE_LOCK_MS) {
        fs.rmSync(paths.lockPath, { force: true });
        continue;
      }
      if (verifyCached(paths).ok) {
        return null;
      }
      await new Promise((resolve) => setTimeout(resolve, 250));
    }
  }
  throw new Error(`Timed out waiting for cache lock: ${paths.lockPath}`);
}

module.exports = { cachePaths, verifyCached, ensureJar, replaceFile };

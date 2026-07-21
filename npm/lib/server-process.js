const { spawn } = require("child_process");

function serverArguments(jarPath, args = []) {
  return [
    "-Dfile.encoding=UTF-8",
    "-jar",
    jarPath,
    ...args,
  ];
}

function startServer(jarPath, cacheDir, args = []) {
  const child = spawn("java", serverArguments(jarPath, args), {
    stdio: "inherit",
    cwd: cacheDir,
  });
  child.on("exit", (code, signal) => {
    if (signal) {
      process.kill(process.pid, signal);
      return;
    }
    process.exit(code || 0);
  });
  return child;
}

module.exports = { serverArguments, startServer };

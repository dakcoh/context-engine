#!/usr/bin/env node

const path = require("path");
const { run } = require("../lib/commands");

const packageRoot = path.resolve(__dirname, "..");
const pkg = require(path.join(packageRoot, "package.json"));

run(process.argv.slice(2), pkg).catch((error) => {
  console.error(error && error.stack ? error.stack : String(error));
  process.exit(1);
});

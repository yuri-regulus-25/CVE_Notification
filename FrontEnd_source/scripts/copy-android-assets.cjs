const fs = require("fs");
const path = require("path");

const projectRoot = path.resolve(__dirname, "..");
const sourceDir = path.join(projectRoot, "dist");
const destinationDir = path.resolve(
  projectRoot,
  "..",
  "CVE_Notification",
  "app",
  "src",
  "main",
  "assets",
  "www",
);

if (!fs.existsSync(sourceDir)) {
  throw new Error(`Build output not found: ${sourceDir}`);
}

fs.mkdirSync(destinationDir, { recursive: true });
fs.cpSync(sourceDir, destinationDir, { recursive: true });

console.log(`Copied ${sourceDir} -> ${destinationDir}`);

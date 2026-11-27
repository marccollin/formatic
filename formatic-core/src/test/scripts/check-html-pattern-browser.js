// Verify the same vectors through actual HTML constraint validation in Chromium.
const fs = require("node:fs");
const path = require("node:path");
const { execFileSync } = require("node:child_process");

const file = path.join(__dirname, "../resources/html-pattern-vectors.tsv");
const lines = fs.readFileSync(file, "utf8").trimEnd().split("\n");
const html = `<!doctype html><body><script>
const lines = ${JSON.stringify(lines)};
let checked = 0;
const failures = [];
for (const line of lines) {
    const [source, input, expected] = line.split("\\t");
    const value = input.startsWith("U+") ? String.fromCodePoint(parseInt(input.slice(2), 16)) : input;
    const element = document.createElement("input");
    element.type = "text";
    element.pattern = source;
    element.value = value;
    // Text inputs sanitize line feeds and carriage returns before validation.
    if (element.value !== value) continue;
    checked++;
    if (element.validity.patternMismatch !== (expected === "false")) failures.push(line);
}
document.body.textContent = failures.length ? "FAIL " + failures.join("; ") : "PASS " + checked;
</script>`;
const url = "data:text/html;charset=utf-8," + encodeURIComponent(html);
const output = execFileSync(process.env.CHROME_BIN || "google-chrome",
    ["--headless", "--disable-gpu", "--dump-dom", url], { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] });
if (!output.includes("<body>PASS ")) {
    throw new Error("Browser pattern validation failed: " + output);
}
console.log(output.match(/<body>(PASS \d+)/)[1]);

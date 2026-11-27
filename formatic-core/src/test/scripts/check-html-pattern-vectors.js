// Compare the Java test vectors with ECMAScript's HTML-pattern Unicode-set mode.
const fs = require("node:fs");
const path = require("node:path");

const file = path.join(__dirname, "../resources/html-pattern-vectors.tsv");
for (const line of fs.readFileSync(file, "utf8").trimEnd().split("\n")) {
    const [source, input, expected] = line.split("\t");
    const value = input.startsWith("U+")
        ? String.fromCodePoint(parseInt(input.slice(2), 16)) : input;
    const actual = new RegExp(`^(?:${source})(?![\\s\\S])`, "v").test(value);
    if (actual !== (expected === "true")) {
        throw new Error(`JavaScript mismatch: ${line}`);
    }
}

# PR Documentation

## Overview

This PR introduces/updates `src/utils.ts`, a collection of general-purpose TypeScript utility functions, along with comprehensive tests in `src/utils.test.ts`.

## Functions Added or Updated

| Function | Signature | Description |
|---|---|---|
| `sum` | `(numbers: number[]) => number` | Returns the sum of an array of numbers; returns `0` for empty arrays. |
| `average` | `(numbers: number[]) => number` | Returns the mean of an array; returns `0` for empty arrays. |
| `formatDate` | `(date: Date) => string` | Returns a `YYYY-MM-DD` string from a `Date` object. |
| `normalizeWhitespace` | `(value: string) => string` | Trims leading/trailing whitespace and collapses internal runs of whitespace to a single space. |
| `isValidEmail` | `(email: string) => boolean` | Validates email addresses using a regex (`user@domain.tld` pattern). |
| `generateRandomString` | `(length: number) => string` | Generates an alphanumeric string of the given length. |
| `deepClone` | `<T>(obj: T) => T` | Returns a deep copy via `JSON.parse(JSON.stringify(...))`. Not suitable for objects with functions, `Date`, `Map`, `Set`, or circular references. |
| `debounce` | `<T extends Function>(func: T, wait: number) => Function` | Returns a debounced version of a function that delays invocation by `wait` ms. |
| `retry` | `<T>(fn: () => Promise<T>, maxRetries?: number, baseDelay?: number) => Promise<T>` | Retries an async function up to `maxRetries` times (default: 3) with exponential backoff starting at `baseDelay` ms (default: 1000). |

## Test Coverage

All functions except `debounce` and `retry` are covered by unit tests in `src/utils.test.ts`. Tests validate:

- Correct output for typical inputs
- Edge cases (empty arrays, boundary values, invalid emails)
- Independence of `deepClone` copies

## Usage Notes

- `deepClone` uses `JSON.parse/stringify` — avoid for non-serializable values.
- `retry` rethrows the last error if all attempts fail.
- `generateRandomString` uses `Math.random()` — not cryptographically secure.

---

## Markdown-to-HTML Report Converter (`scripts/md-to-html-report.groovy`)

A Groovy script for Jenkins Pipelines that converts a Markdown file into a styled, self-contained HTML report suitable for use with the `publishHTML` plugin.

### Usage

Load the script in a Declarative Pipeline and call `convert`:

```groovy
script {
    def report = load 'scripts/md-to-html-report.groovy'
    report.convert('analysis/report.md', 'analysis/report.html', 'Project Analysis Report')
}
```

### `convert(mdPath, htmlPath, title)`

| Parameter | Type | Description |
|-----------|------|-------------|
| `mdPath` | `String` | Workspace-relative path to the Markdown source file |
| `htmlPath` | `String` | Workspace-relative path for the generated HTML output |
| `title` | `String` | Title shown in the HTML page header and `<title>` tag |

### Supported Markdown Features

| Syntax | Rendered as |
|--------|-------------|
| `# H1` … `#### H4` | `<h1>` … `<h4>` |
| `## Section` | Collapsible `<details>` block |
| `- item` | `<ul>` / `<li>` |
| `` `code` `` | Inline `<code>` |
| `**bold**` | `<strong>` |
| ` ```lang ``` ` | `<pre><code class="lang-…">` fenced block |
| `\| table \|` | `<table>` with first row as header |
| `---` | `<hr>` |

### Notes

- All text content is HTML-escaped via `escapeHtml` to prevent XSS in report output.
- `## `headings are wrapped in collapsible `<details open>` sections.
- The script uses `@NonCPS` annotations — safe to call from CPS-transformed Pipeline code.

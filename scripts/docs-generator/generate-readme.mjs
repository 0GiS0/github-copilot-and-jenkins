import { readFileSync, writeFileSync } from 'node:fs';
import { appendSection, firstLines, headLines, readState, runCopilot } from './lib.mjs';

const state = readState();
if (state.skip || !state.pr?.canWrite) {
    process.exit(0);
}

const candidates = state.files?.documentationCandidates || [];
if (!candidates.length) {
    appendSection('README Updates', ['Skipped because no changed files appear to affect README-level documentation.']);
    process.exit(0);
}

let prompt = readFileSync('scripts/docs-generator/prompts/readme.md', 'utf8');
prompt += '\n\nChanged files:\n';
prompt += `${candidates.join('\n')}\n`;
prompt += '\nRelevant file excerpts:\n';
for (const file of candidates) {
    prompt += `\n--- ${file} ---\n${headLines(file, 160)}\n`;
}

writeFileSync('reports/readme-prompt.md', prompt);
const status = runCopilot(prompt, 'reports/readme-copilot-output.txt');
appendSection('README Updates', [firstLines('reports/readme-copilot-output.txt', 120)]);

if (status !== 0) {
    process.exit(status);
}
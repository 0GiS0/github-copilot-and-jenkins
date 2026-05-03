import { readFileSync, writeFileSync } from 'node:fs';
import { appendSection, firstLines, headLines, readState, runCopilot } from './lib.mjs';

const state = readState();
if (state.skip || !state.pr?.canWrite) {
    process.exit(0);
}

const candidates = state.files?.commentCandidates || [];
writeFileSync('reports/api-candidates.txt', candidates.length ? `${candidates.join('\n')}\n` : '');
if (!candidates.length) {
    appendSection('API Documentation', ['Skipped because no changed source files require API documentation.']);
    process.exit(0);
}

let prompt = readFileSync('scripts/docs-generator/prompts/api.md', 'utf8');
prompt += '\n\nChanged source files:\n';
prompt += `${candidates.join('\n')}\n`;
prompt += '\nRelevant file excerpts:\n';
for (const file of candidates) {
    prompt += `\n--- ${file} ---\n${headLines(file, 220)}\n`;
}

writeFileSync('reports/api-prompt.md', prompt);
const status = runCopilot(prompt, 'reports/api-copilot-output.txt');
appendSection('API Documentation', [firstLines('reports/api-copilot-output.txt', 140)]);

if (status !== 0) {
    process.exit(status);
}
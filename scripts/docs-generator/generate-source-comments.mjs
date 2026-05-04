import { readFileSync, writeFileSync } from 'node:fs';
import { appendSection, firstLines, headLines, readState, runCopilot } from './lib.mjs';

const state = readState();
if (state.skip || !state.pr?.canWrite) {
    process.exit(0);
}

const candidates = state.files?.commentCandidates || [];
writeFileSync('reports/source-comment-candidates.txt', candidates.length ? `${candidates.join('\n')}\n` : '');
if (!candidates.length) {
    appendSection('Source Documentation Comments', ['Skipped because no changed source files require documentation comments.']);
    process.exit(0);
}

let prompt = readFileSync('scripts/docs-generator/prompts/source-comments.md', 'utf8');
prompt += '\n\nChanged source files:\n';
prompt += `${candidates.join('\n')}\n`;
prompt += '\nRelevant file excerpts:\n';
for (const file of candidates) {
    prompt += `\n--- ${file} ---\n${headLines(file, 220)}\n`;
}

writeFileSync('reports/source-comments-prompt.md', prompt);
const status = runCopilot(prompt, 'reports/source-comments-copilot-output.txt');
appendSection('Source Documentation Comments', [firstLines('reports/source-comments-copilot-output.txt', 140)]);

if (status !== 0) {
    process.exit(status);
}
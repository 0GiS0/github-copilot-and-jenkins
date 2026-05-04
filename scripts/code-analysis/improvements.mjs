import { writeFileSync } from 'node:fs';
import { appendReport } from '../common/report.mjs';
import { firstLines, headLines, runCopilot } from '../common/copilot.mjs';

const reportPath = 'analysis/report.md';

appendReport(reportPath, '## Improvement Suggestions\n');
appendReport(reportPath, '### General Recommendations\n');

let prompt = headLines('scripts/code-analysis/prompts/improvements.md', 200);
writeFileSync('analysis/copilot-improvements-prompt.txt', prompt);
let status = runCopilot(prompt, 'analysis/copilot-improvements-output.txt');
appendReport(reportPath, firstLines('analysis/copilot-improvements-output.txt', 100));
appendReport(reportPath, '');

if (status !== 0) {
    process.exit(status);
}

appendReport(reportPath, '### Testing Recommendations\n');
prompt = headLines('scripts/code-analysis/prompts/testing.md', 200);
writeFileSync('analysis/copilot-testing-prompt.txt', prompt);
status = runCopilot(prompt, 'analysis/copilot-testing-output.txt');
appendReport(reportPath, firstLines('analysis/copilot-testing-output.txt', 80));
appendReport(reportPath, '');

if (status !== 0) {
    process.exit(status);
}
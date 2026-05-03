import { readFileSync, writeFileSync } from 'node:fs';
import { firstLines, headLines, runCopilot } from '../common/copilot.mjs';
import { appendReport, ensureReportDir, initReport } from '../common/report.mjs';

const reportDir = 'reports';
const reportPath = `${reportDir}/code-review.md`;
const files = readFileSync(`${reportDir}/files-to-review.txt`, 'utf8')
    .split(/\r?\n/)
    .filter(Boolean);

ensureReportDir(reportDir);
initReport(reportPath, 'Code Review Report');

for (const file of files) {
    let prompt = headLines('scripts/code-review/prompts/review.md', 200);
    prompt += `\n\nFile: ${file}\n\n${headLines(file, 100)}\n`;

    writeFileSync(`${reportDir}/copilot-review-prompt.txt`, prompt);
    const outputPath = `${reportDir}/copilot-review-output.txt`;
    const status = runCopilot(prompt, outputPath);

    appendReport(reportPath, `## Reviewing: ${file}\n`);
    appendReport(reportPath, firstLines(outputPath, 120));
    appendReport(reportPath, '\n---\n');

    if (status !== 0) {
        process.exit(status);
    }
}

console.log('Code review complete.');
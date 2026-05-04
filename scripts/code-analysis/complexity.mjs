import { readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { appendReport } from '../common/report.mjs';
import { firstLines, headLines, runCopilot } from '../common/copilot.mjs';

const reportPath = 'analysis/report.md';

function listSourceFiles(dir) {
    return readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
        const path = `${dir}/${entry.name}`;
        if (entry.isDirectory()) {
            return listSourceFiles(path);
        }
        return entry.isFile() && entry.name.endsWith('.ts') && !entry.name.includes('.test.') ? [path] : [];
    });
}

appendReport(reportPath, '## Complexity Analysis\n');

for (const file of listSourceFiles('src').slice(0, 5)) {
    const fullContent = readFileSync(file, 'utf8');
    const content = headLines(file, 80);
    const functionCount = (fullContent.match(/\bfunction\b/g) || []).length;
    const lineCount = fullContent.split(/\r?\n/).length;
    let prompt = readPrompt('scripts/code-analysis/prompts/complexity.md');
    prompt += `\n\nFile: ${file}\n\n${content}\n`;

    writeFileSync('analysis/copilot-complexity-prompt.txt', prompt);
    const status = runCopilot(prompt, 'analysis/copilot-complexity-output.txt');

    appendReport(reportPath, `### ${file}\n`);
    appendReport(reportPath, `- Functions: ${functionCount}`);
    appendReport(reportPath, `- Lines: ${lineCount}\n`);
    appendReport(reportPath, '**Copilot Analysis:**\n');
    appendReport(reportPath, firstLines('analysis/copilot-complexity-output.txt', 80));
    appendReport(reportPath, '');

    if (status !== 0) {
        process.exit(status);
    }
}

function readPrompt(path) {
    return headLines(path, 200);
}
import { readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { appendReport } from '../common/report.mjs';
import { firstLines, headLines, runCopilot } from '../common/copilot.mjs';

const reportPath = 'analysis/report.md';

function listFiles(dir) {
    return readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
        const path = `${dir}/${entry.name}`;
        if (entry.isDirectory()) {
            return listFiles(path);
        }
        return entry.isFile() ? [path] : [];
    });
}

const source = listFiles('src').map(file => readFileSync(file, 'utf8')).join('\n');
appendReport(reportPath, '## Security Analysis\n');
appendReport(reportPath, '### Potential Security Patterns Found\n');

if (source.includes('eval(')) {
    appendReport(reportPath, '- Found `eval()` usage - potential security risk.');
}
if (source.includes('innerHTML')) {
    appendReport(reportPath, '- Found `innerHTML` usage - potential XSS risk.');
}
if (!source.includes('eval(') && !source.includes('innerHTML')) {
    appendReport(reportPath, '- No obvious `eval()` or `innerHTML` usage found in `src/`.');
}

appendReport(reportPath, '\n**Copilot Security Recommendations:**\n');
const prompt = headLines('scripts/code-analysis/prompts/security.md', 200);
writeFileSync('analysis/copilot-security-prompt.txt', prompt);
const status = runCopilot(prompt, 'analysis/copilot-security-output.txt');
appendReport(reportPath, firstLines('analysis/copilot-security-output.txt', 80));
appendReport(reportPath, '');

if (status !== 0) {
    process.exit(status);
}
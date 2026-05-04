import { appendFileSync, mkdirSync, writeFileSync } from 'node:fs';

export function ensureReportDir(reportDir) {
    mkdirSync(reportDir, { recursive: true });
}

export function initReport(reportPath, title) {
    writeFileSync(reportPath, `# ${title}\n\nGenerated: ${new Date().toString()}\nBuild: #${process.env.BUILD_NUMBER || 'local'}\n\n`);
}

export function appendReport(reportPath, text) {
    appendFileSync(reportPath, text.endsWith('\n') ? text : `${text}\n`);
}

export function appendSection(reportPath, title, lines = []) {
    appendReport(reportPath, `## ${title}\n\n`);
    for (const line of lines) {
        appendReport(reportPath, `${line}\n`);
    }
    appendReport(reportPath, '\n');
}
import { readdirSync, readFileSync } from 'node:fs';
import { appendReport, ensureReportDir, initReport } from '../common/report.mjs';

const reportDir = 'analysis';
const reportPath = `${reportDir}/report.md`;

function listTypeScriptFiles(dir) {
    return readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
        const path = `${dir}/${entry.name}`;
        if (entry.isDirectory()) {
            return listTypeScriptFiles(path);
        }
        return entry.isFile() && entry.name.endsWith('.ts') ? [path] : [];
    });
}

ensureReportDir(reportDir);
initReport(reportPath, 'Project Analysis Report');

const files = listTypeScriptFiles('src');
const testFiles = files.filter(file => file.endsWith('.test.ts'));
const totalLines = files.reduce((count, file) => count + readFileSync(file, 'utf8').split(/\r?\n/).length, 0);

appendReport(reportPath, '## Project Statistics\n');
appendReport(reportPath, '| Metric | Value |');
appendReport(reportPath, '|--------|-------|');
appendReport(reportPath, `| TypeScript files | ${files.length} |`);
appendReport(reportPath, `| Test files | ${testFiles.length} |`);
appendReport(reportPath, `| Total lines | ${totalLines} |`);
appendReport(reportPath, '');
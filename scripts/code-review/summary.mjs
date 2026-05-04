import { readFileSync } from 'node:fs';
import { appendReport } from '../common/report.mjs';

const reportPath = 'reports/code-review.md';
const files = readFileSync('reports/files-to-review.txt', 'utf8')
    .split(/\r?\n/)
    .filter(Boolean);

appendReport(reportPath, '## Summary\n');
appendReport(reportPath, `- Files reviewed: ${files.length}`);
appendReport(reportPath, `- Review date: ${new Date().toString()}`);
appendReport(reportPath, `- Jenkins build: #${process.env.BUILD_NUMBER || 'local'}`);
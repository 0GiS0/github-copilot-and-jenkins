import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';

const target = process.env.CHANGE_TARGET;

if (!process.env.CHANGE_ID || !target) {
    throw new Error('CHANGE_ID and CHANGE_TARGET are required. Run this from a Jenkins Pull Request build.');
}

function changedFilesForPullRequest() {
    const output = execFileSync(
        'git',
        ['diff', '--name-only', '--diff-filter=ACMR', `origin/${target}...HEAD`, '--', 'src'],
        { encoding: 'utf8' }
    );

    return output
        .split(/\r?\n/)
        .filter(file => file.endsWith('.ts'))
        .slice(0, 20);
}

const files = changedFilesForPullRequest();
mkdirSync('reports', { recursive: true });
writeFileSync('reports/files-to-review.txt', files.length ? `${files.join('\n')}\n` : '');
console.log('Files to review:');
console.log(files.join('\n'));
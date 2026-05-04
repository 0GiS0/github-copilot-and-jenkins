import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { appendReport } from '../common/report.mjs';

const reportPath = 'reports/code-review.md';
const commentPath = 'reports/pr-comment.md';
const responsePath = 'reports/pr-comment-response.json';
const marker = '<!-- copilot-code-review -->';
const maxCommentLength = 60000;

function run(command, args = []) {
    return execFileSync(command, args, {
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'pipe']
    }).trim();
}

function getRepository() {
    const remote = run('git', ['config', '--get', 'remote.origin.url']);
    let cleaned = remote;
    if (cleaned.startsWith('git@github.com:')) {
        cleaned = cleaned.slice('git@github.com:'.length);
    }
    if (cleaned.startsWith('https://github.com/')) {
        cleaned = cleaned.slice('https://github.com/'.length);
    }
    if (cleaned.includes('@github.com/')) {
        cleaned = cleaned.slice(cleaned.indexOf('@github.com/') + '@github.com/'.length);
    }
    if (cleaned.endsWith('.git')) {
        cleaned = cleaned.slice(0, -4);
    }

    const [owner, name] = cleaned.split('/');
    if (!owner || !name) {
        throw new Error(`Cannot determine GitHub repository from remote URL: ${remote}`);
    }
    return { owner, name };
}

async function githubApi(path, options = {}) {
    if (!process.env.GH_TOKEN) {
        throw new Error('GH_TOKEN is required to publish a Pull Request comment.');
    }

    const response = await fetch(`https://api.github.com${path}`, {
        method: options.method || 'GET',
        headers: {
            Authorization: `Bearer ${process.env.GH_TOKEN}`,
            Accept: 'application/vnd.github+json',
            'Content-Type': 'application/json'
        },
        body: options.body ? JSON.stringify(options.body) : undefined
    });

    const body = await response.text();
    if (!response.ok) {
        throw new Error(`GitHub API ${response.status} for ${path}: ${body}`);
    }
    return body ? JSON.parse(body) : null;
}

function truncateComment(body) {
    if (body.length <= maxCommentLength) {
        return body;
    }

    return `${body.slice(0, maxCommentLength)}\n\n_Comment truncated. See the Jenkins report for the full review._\n`;
}

function buildComment(report, files) {
    const fileLine = files.length
        ? files.map(file => `- \`${file}\``).join('\n')
        : '- No changed TypeScript files were selected for review.';
    const buildUrl = process.env.BUILD_URL || '';
    const buildLine = buildUrl ? `\n- Jenkins build: ${buildUrl}` : '';

    return truncateComment(`${marker}
## Copilot Code Review

Jenkins reviewed the Pull Request changes with GitHub Copilot.

- Pull Request: #${process.env.CHANGE_ID || process.env.PR_NUMBER}${buildLine}
- Files reviewed: ${files.length}

### Files

${fileLine}

### Findings

${report}
`);
}

const prNumber = process.env.CHANGE_ID || process.env.PR_NUMBER;
if (!prNumber) {
    console.log('No PR number available; skipping PR comment.');
    process.exit(0);
}

if (!existsSync(reportPath)) {
    console.log('No code review report available; skipping PR comment.');
    process.exit(0);
}

const files = existsSync('reports/files-to-review.txt')
    ? readFileSync('reports/files-to-review.txt', 'utf8').split(/\r?\n/).filter(Boolean)
    : [];
const report = readFileSync(reportPath, 'utf8');
const body = buildComment(report, files);
writeFileSync(commentPath, body);

const repo = getRepository();
const comments = await githubApi(`/repos/${repo.owner}/${repo.name}/issues/${prNumber}/comments?per_page=100`);
const existing = comments.find(comment => comment.body?.includes(marker));
const response = existing
    ? await githubApi(`/repos/${repo.owner}/${repo.name}/issues/comments/${existing.id}`, { method: 'PATCH', body: { body } })
    : await githubApi(`/repos/${repo.owner}/${repo.name}/issues/${prNumber}/comments`, { method: 'POST', body: { body } });

writeFileSync(responsePath, `${JSON.stringify(response, null, 2)}\n`);
appendReport(reportPath, `\n## Pull Request Comment\n\n${existing ? 'Updated' : 'Posted'} code review comment on PR #${prNumber}.\n`);
console.log(`${existing ? 'Updated' : 'Posted'} code review comment on PR #${prNumber}.`);
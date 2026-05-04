import { existsSync, mkdirSync, readFileSync, writeFileSync, appendFileSync } from 'node:fs';
import { firstLines, headLines, run, runCopilot, runInherited } from '../common/copilot.mjs';

export const reportsDir = 'reports';
export const statePath = `${reportsDir}/docs-generator-state.json`;
export const reportPath = `${reportsDir}/docs-generator.md`;

export function ensureWorkspace() {
    mkdirSync('docs', { recursive: true });
    mkdirSync(reportsDir, { recursive: true });
}

export function initReport() {
    writeFileSync(reportPath, `# Documentation Generator Report\n\nGenerated: ${new Date().toString()}\nBuild: #${process.env.BUILD_NUMBER || 'local'}\n\n`);
}

export function appendReport(text) {
    appendFileSync(reportPath, text.endsWith('\n') ? text : `${text}\n`);
}

export function appendSection(title, lines = []) {
    appendReport(`## ${title}\n\n`);
    for (const line of lines) {
        appendReport(`${line}\n`);
    }
    appendReport('\n');
}

export function readState() {
    return JSON.parse(readFileSync(statePath, 'utf8'));
}

export function writeState(state) {
    writeFileSync(statePath, `${JSON.stringify(state, null, 2)}\n`);
    writeFileSync(`${reportsDir}/pr-context.env`, toEnv({
        SKIP_DOCS_GENERATOR: state.skip ? 'true' : 'false',
        REPO_OWNER: state.repo?.owner || '',
        REPO_NAME: state.repo?.name || '',
        PR_NUMBER: state.pr?.number || '',
        PR_URL: state.pr?.url || '',
        HEAD_BRANCH: state.pr?.headBranch || '',
        BASE_BRANCH: state.pr?.baseBranch || '',
        AUTHOR_LOGIN: state.pr?.author || '',
        AUTHOR_PERMISSION: state.pr?.authorPermission || '',
        IS_CROSS_REPOSITORY: state.pr?.isCrossRepository ? 'true' : 'false',
        CAN_WRITE: state.pr?.canWrite ? 'true' : 'false'
    }));
}

export function writeCandidateFiles(state) {
    writeFileSync(`${reportsDir}/doc-candidates.txt`, `${(state.files?.documentationCandidates || []).join('\n')}\n`.trimStart());
    writeFileSync(`${reportsDir}/comment-candidates.txt`, `${(state.files?.commentCandidates || []).join('\n')}\n`.trimStart());
}

export function toEnv(values) {
    return `${Object.entries(values).map(([key, value]) => `${key}=${String(value).replace(/\n/g, '')}`).join('\n')}\n`;
}

export function latestCommitRequestsSkip() {
    return run('git', ['log', '-1', '--pretty=%B']).includes('[skip docs-generator]');
}

export function getRepository() {
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

export async function githubApi(path, options = {}) {
    const token = process.env.GH_TOKEN;
    if (!token) {
        throw new Error('GH_TOKEN is required');
    }
    const response = await fetch(`https://api.github.com${path}`, {
        method: options.method || 'GET',
        headers: {
            Authorization: `Bearer ${token}`,
            Accept: 'application/vnd.github+json',
            'Content-Type': 'application/json',
            ...(options.headers || {})
        },
        body: options.body ? JSON.stringify(options.body) : undefined
    });
    const body = await response.text();
    if (!response.ok) {
        const error = new Error(`GitHub API ${response.status} for ${path}: ${body}`);
        error.status = response.status;
        throw error;
    }
    return body ? JSON.parse(body) : null;
}

export async function githubApiOr(path, fallback) {
    try {
        return await githubApi(path);
    } catch {
        return fallback;
    }
}

export async function listPullRequestFiles(owner, repo, prNumber) {
    const files = [];
    for (let page = 1; ; page += 1) {
        const pageFiles = await githubApi(`/repos/${owner}/${repo}/pulls/${prNumber}/files?per_page=100&page=${page}`);
        files.push(...pageFiles);
        if (pageFiles.length < 100) {
            return files;
        }
    }
}

export function configureAuthenticatedRemote(owner, repo) {
    runInherited('git', ['remote', 'set-url', 'origin', `https://x-access-token:${process.env.GH_TOKEN}@github.com/${owner}/${repo}.git`]);
}

export { firstLines, headLines, run, runCopilot, runInherited };

export function changedFiles() {
    const tracked = run('git', ['diff', '--name-only'], { stdio: ['ignore', 'pipe', 'pipe'] })
        .split('\n')
        .filter(Boolean);
    const untrackedDocs = run('git', ['ls-files', '--others', '--exclude-standard', 'docs'], { stdio: ['ignore', 'pipe', 'pipe'] })
        .split('\n')
        .filter(Boolean);
    return [...new Set([...tracked, ...untrackedDocs])]
        .filter(file => !file.startsWith('reports/'));
}

export function selectedDocType() {
    return process.env.DOC_TYPE || 'README';
}

export function validateAllowedChanges(state) {
    const allowedSourceFiles = new Set(state.files?.commentCandidates || []);
    const allowMarkdownDocs = ['README', 'ALL'].includes(selectedDocType());
    const files = changedFiles();
    writeFileSync(`${reportsDir}/modified-files.txt`, files.length ? `${files.join('\n')}\n` : '');
    for (const file of files) {
        if (file === 'docs/PR_DOCUMENTATION.md') {
            throw new Error('Copilot created docs/PR_DOCUMENTATION.md, which is not allowed for PR write-back.');
        }
        if (file.startsWith('docs/')) {
            if (allowMarkdownDocs) {
                continue;
            }
            throw new Error(`Copilot created Markdown documentation while DOC_TYPE=${selectedDocType()}: ${file}`);
        }
        if (!allowedSourceFiles.has(file)) {
            throw new Error(`Copilot modified a file outside docs/ and outside the eligible source comment list: ${file}`);
        }
    }
    return files;
}
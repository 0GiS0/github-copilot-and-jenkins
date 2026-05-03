import { existsSync, writeFileSync } from 'node:fs';
import {
    appendSection,
    configureAuthenticatedRemote,
    ensureWorkspace,
    getRepository,
    githubApi,
    githubApiOr,
    initReport,
    latestCommitRequestsSkip,
    listPullRequestFiles,
    runInherited,
    writeCandidateFiles,
    writeState
} from './lib.mjs';

ensureWorkspace();
initReport();

const emptyFiles = { documentationCandidates: [], commentCandidates: [] };

function skipState(reason, repo = {}, pr = {}) {
    const state = { skip: true, reason, repo, pr, files: emptyFiles };
    writeState(state);
    writeCandidateFiles(state);
    appendSection('Result', [reason]);
}

if (latestCommitRequestsSkip()) {
    skipState('Skipped because latest commit contains [skip docs-generator].');
    process.exit(0);
}

const effectivePr = process.env.PR_NUMBER || process.env.CHANGE_ID || '';
if (!effectivePr) {
    skipState('Skipped because no PR was detected. This job only writes documentation for existing PRs.');
    process.exit(0);
}

const repo = getRepository();
const pr = await githubApi(`/repos/${repo.owner}/${repo.name}/pulls/${effectivePr}`);
const permission = await githubApiOr(
    `/repos/${repo.owner}/${repo.name}/collaborators/${pr.user.login}/permission`,
    { permission: 'none', role_name: 'none' }
);

const authorPermission = permission.permission || permission.role_name || 'none';
const allowedPermissions = new Set(['admin', 'maintain', 'write']);
const sameRepository = pr.head.repo.full_name === `${repo.owner}/${repo.name}`;
const canWrite = sameRepository && allowedPermissions.has(authorPermission);

const prState = {
    number: pr.number,
    url: pr.html_url,
    headBranch: pr.head.ref,
    baseBranch: pr.base.ref,
    author: pr.user.login,
    authorPermission,
    isCrossRepository: !sameRepository,
    canWrite,
    writeBackEnabled: process.env.WRITE_BACK_TO_PR || 'true'
};

let state = { skip: false, repo, pr: prState, files: emptyFiles };
writeState(state);
writeCandidateFiles(state);

appendSection('PR Context', [
    `- PR: ${prState.url}`,
    `- Author: ${prState.author}`,
    `- Author repository permission: ${prState.authorPermission}`,
    `- Source branch: ${prState.headBranch}`,
    `- Target branch: ${prState.baseBranch}`,
    `- Cross-repository PR: ${prState.isCrossRepository}`,
    `- Write-back enabled: ${prState.writeBackEnabled}`,
    `- Allowed to generate and push docs: ${prState.canWrite}`
]);

if (!canWrite) {
    appendSection('Result', ['Skipped. Documentation generation only runs for same-repository PRs opened by users with admin, maintain, or write permission.']);
    process.exit(0);
}

configureAuthenticatedRemote(repo.owner, repo.name);
runInherited('git', ['fetch', 'origin', prState.headBranch]);
runInherited('git', ['checkout', '-B', prState.headBranch, `origin/${prState.headBranch}`]);

if (latestCommitRequestsSkip()) {
    state = { ...state, skip: true, reason: 'Skipped because the PR head commit contains [skip docs-generator].' };
    writeState(state);
    writeCandidateFiles(state);
    appendSection('Result', [state.reason]);
    process.exit(0);
}

const prFiles = await listPullRequestFiles(repo.owner, repo.name, pr.number);
writeFileSync('reports/pr-files.json', `${JSON.stringify(prFiles, null, 2)}\n`);
writeFileSync('reports/pr-changed-files.txt', `${prFiles.map(file => file.filename).join('\n')}\n`);

const documentationPattern = /^(src|jenkins-copilot-chat-plugin\/src|pipelines|scripts)\/.*\.(ts|js|java|groovy|jenkinsfile)$|^(README|README\.en|docs)\/.*\.md$/;
const commentPattern = /^(src|jenkins-copilot-chat-plugin\/src)\/.*\.(ts|js|java)$/;
const testPattern = /(^|\.)(test|spec)\.(ts|js|java)$/;

const documentationCandidates = prFiles
    .map(file => file.filename)
    .filter(file => documentationPattern.test(file))
    .filter(file => existsSync(file));
const commentCandidates = documentationCandidates
    .filter(file => commentPattern.test(file))
    .filter(file => !testPattern.test(file));

state = {
    ...state,
    files: { documentationCandidates, commentCandidates }
};
writeState(state);
writeCandidateFiles(state);

appendSection('Changed Files Considered', documentationCandidates.length
    ? documentationCandidates.map(file => `- ${file}`)
    : ['No documentation-relevant changed files were found.']);
appendSection('Source Files Eligible For Documentation Comments', commentCandidates.length
    ? commentCandidates.map(file => `- ${file}`)
    : ['No changed source files were eligible for documentation comments.']);
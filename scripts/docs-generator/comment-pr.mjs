import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { appendSection, githubApi, readState } from './lib.mjs';

if (!existsSync('reports/docs-generator-state.json')) {
    console.log('No PR context available; skipping PR comment.');
    process.exit(0);
}

const state = readState();
if (!state.pr?.number) {
    console.log('No PR number available; skipping PR comment.');
    process.exit(0);
}

let writeBack = { committed: false, commit: '' };
if (existsSync('reports/write-back.json')) {
    writeBack = JSON.parse(readFileSync('reports/write-back.json', 'utf8'));
}

const consideredCount = state.files?.documentationCandidates?.length || 0;
const commentCount = state.files?.commentCandidates?.length || 0;
let resultLine = 'No documentation changes were committed.';
if (writeBack.committed) {
    resultLine = `Committed generated documentation/comment updates: ${writeBack.commit}.`;
} else if (state.skip) {
    resultLine = state.reason || 'Skipped generation.';
} else if (!state.pr.canWrite) {
    resultLine = 'Skipped write-back because the PR author is not allowed or the PR is cross-repository.';
}

const marker = '<!-- copilot-docs-generator -->';
const body = `${marker}\n## Copilot Documentation Generator\n\nJenkins analyzed this PR for documentation updates.\n\n- Build: ${process.env.BUILD_URL || 'local'}\n- Author permission: ${state.pr.authorPermission || 'unknown'}\n- Files considered: ${consideredCount}\n- Source files eligible for comments: ${commentCount}\n- Result: ${resultLine}\n\nSee the Jenkins HTML report for details.\n`;
writeFileSync('reports/pr-comment.md', body);

const comments = await githubApi(`/repos/${state.repo.owner}/${state.repo.name}/issues/${state.pr.number}/comments?per_page=100`);
const existing = comments.find(comment => comment.body?.includes(marker));
const response = existing
    ? await githubApi(`/repos/${state.repo.owner}/${state.repo.name}/issues/comments/${existing.id}`, { method: 'PATCH', body: { body } })
    : await githubApi(`/repos/${state.repo.owner}/${state.repo.name}/issues/${state.pr.number}/comments`, { method: 'POST', body: { body } });

writeFileSync('reports/pr-comment-response.json', `${JSON.stringify(response, null, 2)}\n`);
appendSection('PR Comment', [`${existing ? 'Updated' : 'Posted'} a documentation-generator summary comment on PR #${state.pr.number}.`]);
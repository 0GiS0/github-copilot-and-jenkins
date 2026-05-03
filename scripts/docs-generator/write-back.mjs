import { writeFileSync } from 'node:fs';
import {
    appendSection,
    configureAuthenticatedRemote,
    readState,
    run,
    runInherited,
    validateAllowedChanges
} from './lib.mjs';

const state = readState();
if (state.skip || !state.pr?.canWrite) {
    appendSection('Write-back', ['Skipping write-back because PR author is not allowed, the PR is cross-repository, or generation was skipped.']);
    process.exit(0);
}

if (state.pr.writeBackEnabled === 'false') {
    appendSection('Write-back', ['Skipping write-back because WRITE_BACK_TO_PR is disabled.']);
    process.exit(0);
}

let files = validateAllowedChanges(state);
if (!files.length) {
    writeFileSync('reports/write-back.json', `${JSON.stringify({ committed: false, commit: '' }, null, 2)}\n`);
    writeFileSync('reports/write-back.env', 'DOCS_GENERATOR_COMMITTED=false\nDOCS_GENERATOR_COMMIT=\n');
    appendSection('Write-back', ['No documentation or source-comment changes were generated.']);
    process.exit(0);
}

configureAuthenticatedRemote(state.repo.owner, state.repo.name);
runInherited('git', ['config', 'user.name', 'jenkins-copilot']);
runInherited('git', ['config', 'user.email', 'jenkins-copilot@users.noreply.github.com']);
runInherited('git', ['stash', 'push', '--include-untracked', '-m', 'docs-generator']);
runInherited('git', ['fetch', 'origin', state.pr.headBranch]);
runInherited('git', ['checkout', '-B', state.pr.headBranch, `origin/${state.pr.headBranch}`]);
runInherited('git', ['stash', 'pop']);

files = validateAllowedChanges(state);
for (const file of files) {
    runInherited('git', ['add', file]);
}

runInherited('git', ['commit', '-m', 'docs: update Copilot-generated documentation [skip docs-generator]']);
const generatedCommit = run('git', ['rev-parse', 'HEAD']);
runInherited('git', ['push', 'origin', `HEAD:${state.pr.headBranch}`]);

writeFileSync('reports/write-back.json', `${JSON.stringify({ committed: true, commit: generatedCommit }, null, 2)}\n`);
writeFileSync('reports/write-back.env', `DOCS_GENERATOR_COMMITTED=true\nDOCS_GENERATOR_COMMIT=${generatedCommit}\n`);
appendSection('Write-back', [`Committed generated documentation/comment updates back to PR #${state.pr.number}: ${generatedCommit}.`]);
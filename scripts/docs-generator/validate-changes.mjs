import { appendSection, readState, validateAllowedChanges } from './lib.mjs';

const state = readState();
if (state.skip || !state.pr?.canWrite) {
    process.exit(0);
}

const files = validateAllowedChanges(state);
appendSection('Validation', files.length
    ? [`Validated generated changes: ${files.length} file(s).`]
    : ['No generated file changes found.']);
# PR Documentation: docs-generator.jenkinsfile

## Overview

This PR introduces (or updates) the `pipelines/docs-generator.jenkinsfile` — a declarative Jenkins Pipeline that uses GitHub Copilot CLI to auto-generate and commit documentation for pull requests.

## Pipeline Summary

| Stage | Script | Purpose |
|---|---|---|
| Setup | `scripts/docs-generator/setup.sh` | Verifies GitHub Copilot CLI is installed |
| Prepare PR Context | `scripts/docs-generator/prepare.mjs` | Detects PR context, author permissions, and documentation candidates |
| Generate README Updates | `scripts/docs-generator/generate-readme.mjs` | Generates README-style docs using Copilot (runs when `DOC_TYPE` is `README` or `ALL`) |
| Generate API Docs | `scripts/docs-generator/generate-api.mjs` | Generates API reference docs using Copilot (runs when `DOC_TYPE` is `API` or `ALL`) |
| Validate Generated Changes | `scripts/docs-generator/validate-changes.mjs` | Validates the generated documentation output |
| Commit Documentation to PR | `scripts/docs-generator/write-back.mjs` | Commits generated docs back to the PR branch (conditional on `WRITE_BACK_TO_PR`) |
| Comment on PR | `scripts/docs-generator/comment-pr.mjs` | Posts a documentation summary comment to the PR |

## Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `DOC_TYPE` | Choice (`README`, `API`, `ALL`) | `README` | Selects which documentation type to generate |
| `PR_NUMBER` | String | _(empty)_ | GitHub PR number; auto-populated from `CHANGE_ID` in multibranch builds |
| `WRITE_BACK_TO_PR` | Boolean | `true` | When enabled, commits generated docs back to the PR branch |

## Credentials Required

| Credential ID | Used As | Purpose |
|---|---|---|
| `gh-token` | `GH_TOKEN` | GitHub API access for PR operations |
| `gh-token` | `COPILOT_GITHUB_TOKEN` | Authenticates the GitHub Copilot CLI |

## Post-Build Behaviour

- The pipeline always archives `docs/*.md`, `reports/*.md`, `reports/*.txt`, `reports/*.html`, `reports/*.css`, and `reports/*.json`.
- If `reports/docs-generator.md` exists, it is converted to HTML via `reports/md-to-html-report.groovy` and published as a **Copilot Documentation Generator Report** in the Jenkins build.

## Notable Options

- `ansiColor('xterm')` — coloured console output.
- `timeout(30 MINUTES)` — hard build timeout.
- `disableConcurrentBuilds(abortPrevious: true)` — prevents parallel runs; newer builds cancel older ones.

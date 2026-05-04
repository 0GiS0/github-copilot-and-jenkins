#!/usr/bin/env bash
set -euo pipefail

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI is not available; skipping Git credential helper setup."
  exit 0
fi

gh_path="$(command -v gh)"

git config --global --replace-all credential.https://github.com.helper ""
git config --global --add credential.https://github.com.helper "!${gh_path} auth git-credential"

git config --global --replace-all credential.https://gist.github.com.helper ""
git config --global --add credential.https://gist.github.com.helper "!${gh_path} auth git-credential"

echo "Configured Git credential helper to use ${gh_path}."
#!/usr/bin/env bash
set -euo pipefail

JENKINS_URL="${JENKINS_URL:-http://jenkins:8080}"
JENKINS_USER="${JENKINS_USER:-admin}"
JENKINS_PASSWORD="${JENKINS_PASSWORD:-admin}"
CREDENTIAL_ID="${JENKINS_GH_TOKEN_CREDENTIAL_ID:-gh-token}"
GIT_CREDENTIAL_ID="${JENKINS_GIT_CREDENTIAL_ID:-github-token}"
GIT_USERNAME="${JENKINS_GIT_USERNAME:-0GiS0}"
GITHUB_REPOSITORY="${GITHUB_REPOSITORY:-0GiS0/github-copilot-and-jenkins}"
cookie_jar="$(mktemp)"
trap 'rm -f "${cookie_jar}"' EXIT

if [[ -z "${GH_TOKEN:-}" ]] && command -v gh >/dev/null 2>&1; then
  if token_from_gh="$(gh auth token 2>/dev/null)" && [[ -n "${token_from_gh}" ]]; then
    GH_TOKEN="${token_from_gh}"
  fi
fi

if [[ -z "${GH_TOKEN:-}" ]]; then
  echo "GH_TOKEN is not set and gh auth token is unavailable; skipping Jenkins credential sync."
  exit 0
fi

github_user_status="$(curl -sS -o /dev/null -w '%{http_code}' \
  -H "Authorization: Bearer ${GH_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/user")"

if [[ "${github_user_status}" != "200" ]]; then
  echo "GH_TOKEN is not accepted by GitHub API (/user returned ${github_user_status})." >&2
  exit 1
fi

github_repo_status="$(curl -sS -o /dev/null -w '%{http_code}' \
  -H "Authorization: Bearer ${GH_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/${GITHUB_REPOSITORY}")"

if [[ "${github_repo_status}" != "200" ]]; then
  echo "GH_TOKEN cannot access ${GITHUB_REPOSITORY} (repo API returned ${github_repo_status})." >&2
  exit 1
fi

echo "Syncing Jenkins GitHub credentials..."

credential_id_base64="$(printf '%s' "${CREDENTIAL_ID}" | base64 | tr -d '\n')"
git_credential_id_base64="$(printf '%s' "${GIT_CREDENTIAL_ID}" | base64 | tr -d '\n')"
git_username_base64="$(printf '%s' "${GIT_USERNAME}" | base64 | tr -d '\n')"
token_base64="$(printf '%s' "${GH_TOKEN}" | base64 | tr -d '\n')"

for attempt in {1..60}; do
  if curl -fsS -u "${JENKINS_USER}:${JENKINS_PASSWORD}" -c "${cookie_jar}" -b "${cookie_jar}" "${JENKINS_URL}/login" >/dev/null; then
    break
  fi

  if [[ "${attempt}" -eq 60 ]]; then
    echo "Jenkins did not become available at ${JENKINS_URL}." >&2
    exit 1
  fi

  sleep 2
done

crumb_json="$(curl -fsS -u "${JENKINS_USER}:${JENKINS_PASSWORD}" -c "${cookie_jar}" -b "${cookie_jar}" "${JENKINS_URL}/crumbIssuer/api/json")"
crumb_field="$(printf '%s' "${crumb_json}" | sed -n 's/.*"crumbRequestField":"\([^"]*\)".*/\1/p')"
crumb_value="$(printf '%s' "${crumb_json}" | sed -n 's/.*"crumb":"\([^"]*\)".*/\1/p')"

if [[ -z "${crumb_field}" || -z "${crumb_value}" ]]; then
  echo "Could not read Jenkins crumb." >&2
  exit 1
fi

groovy_script="$(cat <<'GROOVY'
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import hudson.util.Secret
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import org.jenkinsci.plugins.plaincredentials.StringCredentials
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl

def credentialId = new String('__CREDENTIAL_ID_BASE64__'.decodeBase64(), 'UTF-8')
def gitCredentialId = new String('__GIT_CREDENTIAL_ID_BASE64__'.decodeBase64(), 'UTF-8')
def gitUsername = new String('__GIT_USERNAME_BASE64__'.decodeBase64(), 'UTF-8')
def token = new String('__TOKEN_BASE64__'.decodeBase64(), 'UTF-8')

if (!token) {
  throw new IllegalStateException('SYNC_GH_TOKEN is empty')
}

def store = SystemCredentialsProvider.getInstance().getStore()
def domain = Domain.global()
def existing = CredentialsProvider.lookupCredentialsInItemGroup(
  StringCredentials.class,
  Jenkins.instance,
  null,
  null
).find { it.id == credentialId }
def replacement = new StringCredentialsImpl(
  CredentialsScope.GLOBAL,
  credentialId,
  'GitHub Token for Copilot CLI',
  Secret.fromString(token)
)

if (existing) {
  store.updateCredentials(domain, existing, replacement)
  println "Updated credential ${credentialId}"
} else {
  store.addCredentials(domain, replacement)
  println "Created credential ${credentialId}"
}

def existingGitCredential = CredentialsProvider.lookupCredentialsInItemGroup(
  StandardUsernamePasswordCredentials.class,
  Jenkins.instance,
  null,
  null
).find { it.id == gitCredentialId }
def gitReplacement = new UsernamePasswordCredentialsImpl(
  CredentialsScope.GLOBAL,
  gitCredentialId,
  'GitHub token for private repository checkout',
  gitUsername,
  token
)

if (existingGitCredential) {
  store.updateCredentials(domain, existingGitCredential, gitReplacement)
  println "Updated credential ${gitCredentialId}"
} else {
  store.addCredentials(domain, gitReplacement)
  println "Created credential ${gitCredentialId}"
}

SystemCredentialsProvider.getInstance().save()
GROOVY
)"

groovy_script="${groovy_script/__CREDENTIAL_ID_BASE64__/${credential_id_base64}}"
groovy_script="${groovy_script/__GIT_CREDENTIAL_ID_BASE64__/${git_credential_id_base64}}"
groovy_script="${groovy_script/__GIT_USERNAME_BASE64__/${git_username_base64}}"
groovy_script="${groovy_script/__TOKEN_BASE64__/${token_base64}}"

curl -fsS -u "${JENKINS_USER}:${JENKINS_PASSWORD}" \
  -c "${cookie_jar}" \
  -b "${cookie_jar}" \
  -H "${crumb_field}: ${crumb_value}" \
  --data-urlencode "script=${groovy_script}" \
  "${JENKINS_URL}/scriptText"
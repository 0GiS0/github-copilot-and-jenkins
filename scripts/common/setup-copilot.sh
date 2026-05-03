set -eu

REPORT_DIR="${1:-reports}"
mkdir -p "$REPORT_DIR"
cp scripts/md-to-html-report.groovy "$REPORT_DIR/md-to-html-report.groovy"
cp scripts/report.css "$REPORT_DIR/report-source.css"

node -e "const major = Number(process.versions.node.split('.')[0]); if (major < 18) { console.error('Node 18 or newer is required for docs-generator scripts. Current: ' + process.version); process.exit(1); }"

if ! command -v copilot >/dev/null 2>&1; then
    echo "Installing GitHub Copilot CLI..."
    curl -fsSL https://gh.io/copilot-install | PREFIX="$HOME/.local" bash
fi

copilot --version
echo "GitHub Copilot CLI is ready"
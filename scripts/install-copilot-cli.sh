#!/bin/bash
# Script to install and configure GitHub Copilot CLI

set -e

echo "🚀 Installing GitHub Copilot CLI..."

# Check if gh is installed
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) is not installed. Please install it first."
    exit 1
fi

# Check if GH_TOKEN is set
if [ -z "$GH_TOKEN" ]; then
    echo "⚠️  GH_TOKEN environment variable is not set."
    echo "   Please set it with a token that has 'copilot' scope."
    echo ""
    echo "   Example: export GH_TOKEN='your_github_token'"
    exit 1
fi

# Install GitHub Copilot CLI extension
echo "📦 Installing gh-copilot extension..."
gh extension install github/gh-copilot 2>/dev/null || {
    echo "ℹ️  Extension already installed, upgrading..."
    gh extension upgrade gh-copilot || true
}

# Verify installation
echo ""
echo "✅ GitHub Copilot CLI installed successfully!"
echo ""
echo "🔧 Available commands:"
echo "   gh copilot suggest  - Get command suggestions"
echo "   gh copilot explain  - Explain a command"
echo ""
echo "📖 Usage in Jenkins pipelines:"
echo '   sh "echo \"How to list files recursively\" | gh copilot suggest -t shell"'
echo ""
echo "🎉 Setup complete!"

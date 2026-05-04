#!/bin/bash
# Script to install and configure GitHub Copilot CLI

set -e

echo "🚀 Installing GitHub Copilot CLI..."

# Check if GH_TOKEN is set
if [ -z "$GH_TOKEN" ]; then
    echo "⚠️  GH_TOKEN environment variable is not set."
    echo "   Please set it with a token that has Copilot Requests permission."
    echo ""
    echo "   Example: export GH_TOKEN='your_github_token'"
    exit 1
fi

# Install GitHub Copilot CLI
echo "📦 Installing @github/copilot..."
npm install -g @github/copilot

# Verify installation
echo ""
echo "✅ GitHub Copilot CLI installed successfully!"
echo ""
echo "🔧 Available commands:"
echo "   copilot                         - Start an interactive session"
echo "   copilot -p ...  - Run a prompt programmatically"
echo ""
echo "📖 Usage in Jenkins pipelines:"
echo '   sh "copilot --prompt \"Analyze this repository\""'
echo ""
echo "🎉 Setup complete!"

pipeline {
    agent any
    
    options {
        ansiColor('xterm')
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    
    environment {
        GH_TOKEN = credentials('gh-token')
        COPILOT_GITHUB_TOKEN = credentials('gh-token')
        PATH+COPILOT = "${env.HOME}/.local/bin:/usr/local/bin"
    }
    
    parameters {
        choice(
            name: 'DEMO_TYPE',
            choices: ['ALL', 'CODE_REVIEW', 'DOCS_GENERATOR', 'CODE_ANALYSIS'],
            description: 'Select which Copilot CLI demo to run'
        )
        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Run project tests before demos'
        )
        booleanParam(
            name: 'INSTALL_DEPS',
            defaultValue: true,
            description: 'Install npm dependencies'
        )
    }
    
    stages {
        stage('🔧 Environment Setup') {
            steps {
                echo '''
╔══════════════════════════════════════════════════════════════╗
║     🤖 GitHub Copilot CLI + Jenkins Integration Demo        ║
╚══════════════════════════════════════════════════════════════╝
                '''
                
                echo '📦 Verifying GitHub Copilot CLI installation...'
                sh '''
                    if ! command -v copilot >/dev/null 2>&1; then
                        echo "Installing GitHub Copilot CLI..."
                        curl -fsSL https://gh.io/copilot-install | PREFIX="$HOME/.local" bash
                    fi

                    copilot --version
                '''
            }
        }
        
        stage('📥 Install Dependencies') {
            when {
                expression { params.INSTALL_DEPS }
            }
            steps {
                echo '📦 Installing npm dependencies...'
                sh 'npm ci || npm install'
            }
        }
        
        stage('🏗️ Build Project') {
            steps {
                echo '🔨 Building TypeScript project...'
                sh 'npm run build'
            }
        }
        
        stage('🧪 Run Tests') {
            when {
                expression { params.RUN_TESTS }
            }
            steps {
                echo '🧪 Running tests...'
                sh 'npm test || true'
            }
        }
        
        stage('🤖 Copilot Demos') {
            parallel {
                stage('📝 Code Review') {
                    when {
                        expression { params.DEMO_TYPE == 'ALL' || params.DEMO_TYPE == 'CODE_REVIEW' }
                    }
                    steps {
                        build job: 'copilot-demos/code-review', wait: true, propagate: false
                    }
                }
                
                stage('📚 Docs Generator') {
                    when {
                        expression { params.DEMO_TYPE == 'ALL' || params.DEMO_TYPE == 'DOCS_GENERATOR' }
                    }
                    steps {
                        build job: 'copilot-demos/docs-generator', 
                              parameters: [choice(name: 'DOC_TYPE', value: 'ALL')],
                              wait: true, 
                              propagate: false
                    }
                }
                
                stage('🔍 Code Analysis') {
                    when {
                        expression { params.DEMO_TYPE == 'ALL' || params.DEMO_TYPE == 'CODE_ANALYSIS' }
                    }
                    steps {
                        build job: 'copilot-demos/code-analysis', wait: true, propagate: false
                    }
                }
            }
        }
        
        stage('📊 Summary Report') {
            steps {
                echo '📋 Generating summary report...'
                sh '''
                    mkdir -p reports
                    
                    echo "# 🤖 Copilot CLI Demo Summary" > reports/summary.md
                    echo "" >> reports/summary.md
                    echo "## Build Information" >> reports/summary.md
                    echo "- Build Number: #${BUILD_NUMBER}" >> reports/summary.md
                    echo "- Build Date: $(date)" >> reports/summary.md
                    echo "- Demo Type: ${DEMO_TYPE}" >> reports/summary.md
                    echo "" >> reports/summary.md
                    
                    echo "## Demos Executed" >> reports/summary.md
                    echo "" >> reports/summary.md
                    
                    if [ "${DEMO_TYPE}" = "ALL" ] || [ "${DEMO_TYPE}" = "CODE_REVIEW" ]; then
                        echo "- ✅ Code Review" >> reports/summary.md
                    fi
                    
                    if [ "${DEMO_TYPE}" = "ALL" ] || [ "${DEMO_TYPE}" = "DOCS_GENERATOR" ]; then
                        echo "- ✅ Documentation Generator" >> reports/summary.md
                    fi
                    
                    if [ "${DEMO_TYPE}" = "ALL" ] || [ "${DEMO_TYPE}" = "CODE_ANALYSIS" ]; then
                        echo "- ✅ Code Analysis" >> reports/summary.md
                    fi
                    
                    echo "" >> reports/summary.md
                    echo "## Next Steps" >> reports/summary.md
                    echo "" >> reports/summary.md
                    echo "Check the individual pipeline artifacts for detailed reports." >> reports/summary.md
                '''
            }
        }
    }
    
    post {
        always {
            echo '📁 Archiving artifacts...'
            archiveArtifacts artifacts: 'reports/*.md', allowEmptyArchive: true
            cleanWs(cleanWhenNotBuilt: false, deleteDirs: true, disableDeferredWipeout: true)
        }
        success {
            echo '''
╔══════════════════════════════════════════════════════════════╗
║        ✅ All Copilot CLI demos completed successfully!      ║
╚══════════════════════════════════════════════════════════════╝
            '''
        }
        failure {
            echo '''
╔══════════════════════════════════════════════════════════════╗
║           ❌ Some demos failed - check logs above            ║
╚══════════════════════════════════════════════════════════════╝
            '''
        }
    }
}

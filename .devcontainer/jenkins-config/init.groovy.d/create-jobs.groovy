import jenkins.model.Jenkins
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.*
import hudson.model.*

def repositoryUrl = 'https://github.com/0GiS0/github-copilot-and-jenkins.git'
def gitCredentialsId = 'github-token'

def jobDsl = """
folder('copilot-demos') {
    displayName('Copilot CLI Demos')
    description('Pipelines demonstrating GitHub Copilot CLI integration')
}

pipelineJob('copilot-demos/code-review') {
    displayName('Code Review with Copilot')
    description('Automated code review using GitHub Copilot CLI')
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('${repositoryUrl}')
                        credentials('${gitCredentialsId}')
                    }
                    branches('*/main')
                }
            }
            scriptPath('pipelines/code-review.jenkinsfile')
        }
    }
}

pipelineJob('copilot-demos/docs-generator') {
    displayName('Documentation Generator')
    description('Generate documentation using GitHub Copilot CLI')
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('${repositoryUrl}')
                        credentials('${gitCredentialsId}')
                    }
                    branches('*/main')
                }
            }
            scriptPath('pipelines/docs-generator.jenkinsfile')
        }
    }
}

pipelineJob('copilot-demos/code-analysis') {
    displayName('Code Analysis')
    description('Analyze code and get suggestions using GitHub Copilot CLI')
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('${repositoryUrl}')
                        credentials('${gitCredentialsId}')
                    }
                    branches('*/main')
                }
            }
            scriptPath('pipelines/code-analysis.jenkinsfile')
        }
    }
}

"""

def jenkins = Jenkins.instance
def workspace = new File(System.getProperty("java.io.tmpdir"), "jobdsl-workspace")
workspace.mkdirs()

def jobManagement = new JenkinsJobManagement(System.out, [:], workspace)
new DslScriptLoader(jobManagement).runScript(jobDsl)
jenkins.save()

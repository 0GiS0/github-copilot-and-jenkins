import jenkins.model.Jenkins
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.*
import hudson.model.*

def repositoryUrl = 'https://github.com/0GiS0/github-copilot-and-jenkins.git'
def gitCredentialsId = 'github-token'
def repositoryOwner = '0GiS0'
def repositoryName = 'github-copilot-and-jenkins'
def codeReviewJobName = 'copilot-demos/code-review'

def jobDsl = """
folder('copilot-demos') {
    displayName('Copilot CLI Demos')
    description('Pipelines demonstrating GitHub Copilot CLI integration')
}

multibranchPipelineJob('${codeReviewJobName}') {
    displayName('Code Review with Copilot')
    description('Automated Pull Request code review using GitHub Copilot CLI')
    branchSources {
        branchSource {
            source {
                github {
                    id('copilot-demos-code-review')
                    credentialsId('${gitCredentialsId}')
                    repositoryUrl('${repositoryUrl}')
                    configuredByUrl(false)
                    repoOwner('${repositoryOwner}')
                    repository('${repositoryName}')
                    traits {
                        gitHubPullRequestDiscovery {
                            strategyId(1)
                        }
                    }
                }
            }
        }
    }
    factory {
        workflowBranchProjectFactory {
            scriptPath('pipelines/code-review.jenkinsfile')
        }
    }
    triggers {
        periodicFolderTrigger {
            interval('1h')
        }
    }
    orphanedItemStrategy {
        discardOldItems {
            numToKeep(20)
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
def existingCodeReviewJob = jenkins.getItemByFullName(codeReviewJobName)
if (existingCodeReviewJob && existingCodeReviewJob.class.name != 'org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject') {
    existingCodeReviewJob.delete()
}

def workspace = new File(System.getProperty("java.io.tmpdir"), "jobdsl-workspace")
workspace.mkdirs()

def jobManagement = new JenkinsJobManagement(System.out, [:], workspace)
new DslScriptLoader(jobManagement).runScript(jobDsl)
jenkins.save()

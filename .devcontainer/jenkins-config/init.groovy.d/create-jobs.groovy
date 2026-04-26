import jenkins.model.Jenkins
import javaposse.jobdsl.plugin.*
import hudson.model.*

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
                        url('/workspace')
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
                        url('/workspace')
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
                        url('/workspace')
                    }
                    branches('*/main')
                }
            }
            scriptPath('pipelines/code-analysis.jenkinsfile')
        }
    }
}

pipelineJob('main-pipeline') {
    displayName('Main Pipeline - All Demos')
    description('Run all Copilot CLI demonstrations')
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('/workspace')
                    }
                    branches('*/main')
                }
            }
            scriptPath('Jenkinsfile')
        }
    }
}
"""

def jenkins = Jenkins.instance
def workspace = new File(System.getProperty("java.io.tmpdir"), "jobdsl-workspace")
workspace.mkdirs()

def scriptFile = new File(workspace, "jobs.groovy")
scriptFile.text = jobDsl

def seedJob = jenkins.createProject(FreeStyleProject, "seed-job-init")
seedJob.buildersList.add(new ExecuteDslScripts(
    new ExecuteDslScripts.ScriptLocation(null, null, jobDsl),
    false,
    RemovedJobAction.DELETE,
    RemovedViewAction.DELETE,
    LookupStrategy.JENKINS_ROOT
))

def cause = new Cause.UserIdCause()
seedJob.scheduleBuild(0, cause)

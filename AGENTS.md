# Agent Instructions

## Workflow: GitHub Flow

For **every task** that involves code changes, follow [GitHub Flow](https://docs.github.com/en/get-started/using-github/github-flow):

1. **Create a branch** from `main` with a descriptive name (e.g., `feature/add-login`, `fix/null-pointer`)
2. **Make commits** with clear, conventional commit messages
3. **Open a Pull Request** when the work is ready for review
4. **Request review** and address feedback
5. **Merge** to `main` after approval

For small, isolated changes (documentation fixes, formatting), direct commits to `main` are acceptable.

## Specialist Agents

Delegate to the appropriate expert agent based on the task:

### ☕ Java Expert (`@java-expert`)
**Invoke for:**
- Java source code (`.java` files)
- Java 17 features (records, sealed classes, pattern matching)
- Maven/Gradle configuration for Java projects
- JUnit testing
- Design patterns and best practices

### ⚙️ Groovy Expert (`@groovy-expert`)
**Invoke for:**
- Groovy scripts (`.groovy` files)
- Jenkins Pipeline DSL (Declarative and Scripted)
- Shared Libraries (`vars/`, `src/`)
- Job DSL scripts
- CPS (Continuation Passing Style) issues
- Sandbox security and script approvals

### 🚀 Jenkins Expert (`@jenkins-expert`)
**Invoke for:**
- Jenkinsfile authoring and debugging
- Jenkins administration and configuration
- Plugin configuration and troubleshooting
- Configuration as Code (JCasC)
- Build failures and pipeline optimization
- CI/CD pipeline design

## Decision Matrix

| File/Task | Agent |
|-----------|-------|
| `*.java` in `src/` | ☕ Java Expert |
| `*.groovy` in `vars/` or `src/` | ⚙️ Groovy Expert |
| `Jenkinsfile`, `*.jenkinsfile` | 🚀 Jenkins Expert |
| Pipeline syntax questions | 🚀 Jenkins Expert |
| CPS/Sandbox errors | ⚙️ Groovy Expert |
| Maven `pom.xml` for Java | ☕ Java Expert |
| Jenkins plugin development | ☕ Java Expert + 🚀 Jenkins Expert |

## Example Delegation

```
User: "Fix the NullPointerException in CopilotChatSessionService.java"
→ Delegate to @java-expert

User: "Why is my pipeline failing with NotSerializableException?"
→ Delegate to @groovy-expert (CPS issue)

User: "Set up a multibranch pipeline for this repo"
→ Delegate to @jenkins-expert
```

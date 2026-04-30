# 🤖 Copilot Chat Plugin — Internals

This document explains how the **Copilot Chat** Jenkins plugin is architected and how each
piece of the Jenkins plugin system (described in [how-jenkins-plugins-work.md](./how-jenkins-plugins-work.md))
was used to build it.

---

## 🗺️ Architecture Overview

```
Browser (chat widget)
   │  HTTP / Server-Sent Events (SSE)
   ▼
CopilotChatRootAction        ← Stapler RootAction  (REST endpoints)
   │                            + CopilotChatPageDecorator (injects JS/CSS)
   │
   ├── DeviceFlowAuthService  ← GitHub OAuth 2.0 Device Flow
   │      └── GitHubTokenStore  ← persists token as Jenkins UserProperty
   │
   └── CopilotChatSessionService  ← manages per-user AI sessions
          └── CopilotClientFactory  ← creates Copilot SDK clients
                 └── CopilotClient (SDK)
                        └── CopilotSession (SDK)
                               ├── Jenkins MCP server  ← calls Jenkins API tools
                               └── GitHub MCP server   ← calls GitHub API tools
```

---

## 🧩 How Each Jenkins Building Block Was Used

### `RootAction` → REST API + Sidebar Link

`CopilotChatRootAction` implements `RootAction` which gives us:
- A URL namespace at `/copilot-chat/`
- A sidebar link with a custom icon (SVG symbol registered in `webapp/images/symbols/`)
- Public `doXxx()` methods that Stapler maps to HTTP endpoints automatically

```
GET  /copilot-chat/startLogin   → doStartLogin()
GET  /copilot-chat/pollLogin    → doPollLogin(loginId)
GET  /copilot-chat/authStatus   → doAuthStatus()
GET  /copilot-chat/logout       → doLogout()
GET  /copilot-chat/models       → doModels()
POST /copilot-chat/sendMessage  → doSendMessage(request, response)
```

No routing table or Spring configuration — just method naming conventions.

---

### `PageDecorator` → Injecting the Chat Widget

`CopilotChatPageDecorator` is an empty class extending `PageDecorator`.
The real work happens in `CopilotChatPageDecorator/footer.jelly`, which Jenkins
renders at the bottom of **every page**. The Jelly template:

1. Loads `copilot-chat.css` and `copilot-chat.js` from `webapp/`.
2. Renders the chat widget `<div>` that the JavaScript attaches to.

Result: the chat bubble appears on all Jenkins pages without changing any core Jenkins code.

---

### `GlobalConfiguration` → Plugin Settings

`CopilotChatConfiguration` stores all plugin settings in `$JENKINS_HOME/copilotChatConfiguration.xml`:

| Field | Purpose |
|-------|---------|
| `clientId` | GitHub OAuth App client ID for Device Flow |
| `cliUrl` | URL of the remote Copilot CLI HTTP server |
| `cliPath` | Path to a local Copilot CLI binary (alternative) |
| `defaultModel` | AI model to use (e.g. `gpt-5.4`) |
| `availableTools` | Comma-separated MCP tools the AI may call |
| `requestTimeoutSeconds` | Streaming timeout |
| `jenkinsMcpUrl/Username/Token` | Credentials for the Jenkins MCP server |
| `githubMcpUrl/Token` | Credentials for the GitHub MCP server |

The `config.jelly` template renders all these fields as a form in *Manage Jenkins → System*.
`@DataBoundSetter` methods are called automatically when the user saves the form.

---

### `UserProperty` → Storing GitHub Tokens Per User

`CopilotTokenUserProperty` stores three values per Jenkins user:
- The GitHub OAuth **access token** (encrypted with Jenkins `Secret`)
- The GitHub **login** (username string)
- The numeric GitHub **user ID**

`GitHubTokenStore` acts as a DAO on top of this:
```java
tokenStore.save(user, accessToken, login, id);   // after OAuth success
tokenStore.getToken(user);                         // before each API call
tokenStore.delete(user);                           // on logout
```

Tokens are encrypted at rest — Jenkins uses AES with a master key stored in `$JENKINS_HOME/secrets/`.

---

## 🔑 GitHub OAuth 2.0 Device Flow

The plugin uses the **Device Authorization Grant** (RFC 8628) — the OAuth flow designed for
devices that can't open a browser. This is ideal here because the Jenkins server itself
initiates the flow on behalf of the browser.

```
Browser            Jenkins server         GitHub
   │                    │                    │
   │── Click Login ────►│                    │
   │                    │── POST /device/code►│
   │                    │◄── {userCode, uri} ─│
   │◄── {loginId,      │                    │
   │     userCode,      │                    │
   │     verificationUri}                    │
   │                    │                    │
   │  (user visits verificationUri, types userCode on GitHub)
   │                    │                    │
   │── Poll every 5s ──►│── POST /token ─────►│
   │                    │◄── authorization_pending
   │── Poll ────────────►│── POST /token ─────►│
   │                    │◄── {access_token} ──│
   │◄── {authenticated} │                    │
   │    login, id       │── GET /user ────────►│
   │                    │◄── {login, id} ─────│
   │                    │                    │
   │                    │ save token to UserProperty
```

Key implementation detail: `pendingLogins` is a `ConcurrentHashMap` that stores
in-flight login state between the start and poll calls. It's keyed by a random UUID
(`loginId`) so multiple users can log in concurrently.

---

## 💬 Session Management (`CopilotChatSessionService`)

Each Jenkins user gets one long-lived `UserChatSession` that survives across multiple
chat turns (maintaining conversation history):

```java
private record UserChatSession(
    CopilotClient client,    // connection to the Copilot CLI
    CopilotSession session,  // stateful AI conversation
    String model             // which model this session uses
) {}
```

Sessions are cached in a `ConcurrentHashMap<String, UserChatSession>` keyed by Jenkins user ID.

**Session creation steps:**
1. `CopilotClientFactory` creates a `CopilotClient` (either pointing at the remote CLI server or a local binary).
2. `client.start()` starts the CLI process / connects to the remote server.
3. `client.createSession(config)` opens an AI conversation with:
   - The chosen model
   - Streaming enabled
   - A system message describing Jenkins context and available tools
   - MCP server registrations (Jenkins + optionally GitHub)
4. A `CountDownLatch` waits for MCP servers to finish loading before allowing messages.

**Model switching:** if the user selects a different model, the old session is stopped and a new one is created transparently.

---

## 🌐 MCP Server Integration

MCP (Model Context Protocol) servers expose operations as **tools** the AI can call autonomously.
Two MCP servers are registered per session:

### 🏗️ Jenkins MCP (`jenkins`)
Always registered. Exposes tools for:
- Reading jobs, builds, logs, test results
- Triggering builds, replaying pipelines
- Finding SCM (Git) URLs for jobs

Authentication: HTTP Basic (username + API token encoded as Base64).

### 🐙 GitHub MCP (`github`)
Registered only when configured. Exposes tools for:
- Reading and editing files in repositories
- Creating branches and pull requests
- Searching issues and code

Authentication: HTTP Bearer token (GitHub Personal Access Token).

This combination lets the AI answer questions like:
> *"Why did the last build fail?"* → calls `getBuildLog()`  
> *"Fix the Jenkinsfile and open a PR"* → calls `getJobScm()` + `createBranch()` + `editFile()` + `createPullRequest()`

---

## 📡 Streaming Responses (Server-Sent Events)

The AI response is streamed token-by-token using **Server-Sent Events (SSE)**.

### Why SSE?
- Native browser support (`EventSource` API or `fetch()` with streaming)
- One-directional server→client push over a standard HTTP connection
- Works through Jenkins' existing HTTP stack without WebSocket setup

### How it works

`StreamingHttpResponse` sets response headers and keeps the connection open:
```
Content-Type: text/event-stream
Cache-Control: no-cache
X-Accel-Buffering: no   ← disables nginx buffering
```

The Copilot SDK fires events as they arrive from the AI model. The plugin registers
four listeners per chat turn:

| SDK Event | SSE event sent to browser |
|-----------|--------------------------|
| `AssistantReasoningEvent` | `{"type":"reasoning","content":"..."}` |
| `AssistantMessageDeltaEvent` | `{"type":"delta","content":"..."}` |
| `AssistantMessageEvent` | (fallback, if no deltas) |
| `SessionIdleEvent` | `{"type":"complete"}` |
| `SessionErrorEvent` | `{"type":"error","message":"..."}` |

The JavaScript in `copilot-chat.js` reads these events and appends deltas to the chat bubble in real time.

Each event listener is stored as a `Closeable` and removed after the turn completes (or errors)
to avoid memory leaks from lingering subscriptions.

---

## 🔌 Copilot SDK Client Modes

`CopilotClientFactory` supports two deployment modes:

| Mode | When to use | How it works |
|------|------------|-------------|
| **Remote CLI server** | Docker / Kubernetes | Set `cliUrl`. The SDK sends HTTP requests to a running `copilot-cli` container. No GitHub token needed on the Jenkins side. |
| **Local CLI** | Development / bare metal | Leave `cliUrl` blank. The SDK spawns a local `copilot` CLI process using the user's GitHub token. Optionally set `cliPath` for a specific binary. |

The recommended setup (used in the Dev Container) is the remote CLI server mode — the `copilot-cli` service in `docker-compose.yml` handles all Copilot authentication and model access.

---

## 🛡️ Security Considerations

- **Token encryption**: GitHub tokens are stored as `hudson.util.Secret`, AES-encrypted with Jenkins' master key.
- **Permission check**: every endpoint calls `Jenkins.get().checkPermission(Jenkins.READ)` before processing.
- **User isolation**: sessions and tokens are keyed by Jenkins user ID — one user cannot access another's session or token.
- **No token exposure**: `CopilotClientOptions.setUseLoggedInUser(false)` prevents the SDK from reading tokens from the system Git config.

---

## 📖 Related Documentation

- [How Jenkins Plugins Work](./how-jenkins-plugins-work.md)
- [Jenkins Plugin Developer Guide](https://www.jenkins.io/doc/developer/)
- [GitHub Device Flow (RFC 8628)](https://datatracker.ietf.org/doc/html/rfc8628)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Server-Sent Events spec](https://html.spec.whatwg.org/multipage/server-sent-events.html)

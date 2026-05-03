# 🔌 How Jenkins Plugins Work

🇪🇸 [Leer en Español](how-jenkins-plugins-work.md)

This document explains the key concepts behind Jenkins plugin development.
Understanding these building blocks is essential to follow how the **GitHubCopilot Chat** plugin is structured.

---

## 📦 What is a Jenkins Plugin?

A Jenkins plugin is a `.hpi` (Hudson Plugin Interface) or `.jpi` file — essentially a JAR with extra metadata packed inside a WAR-like structure. When Jenkins starts, it unpacks all plugins found in `$JENKINS_HOME/plugins/` and loads them into its classloader.

The project is built with **Maven** using the `jenkins-plugin` POM parent, which provides:
- A pre-configured `hpi:hpi` Maven goal that packages everything.
- Dependency management for the Jenkins core API.
- A `WEB-INF/lib/` directory for plugin JARs.
- A `META-INF/MANIFEST.MF` with plugin metadata (name, version, dependencies).

```
plugin.hpi
├── META-INF/
│   └── MANIFEST.MF          ← plugin metadata
├── WEB-INF/
│   └── lib/
│       └── my-plugin.jar    ← compiled Java classes + Jelly views
└── images/, css/, js/       ← static assets (webapp/)
```

---

## 🔧 Extension Points — `@Extension`

The core of Jenkins' plugin system is its **extension point** model.
Jenkins defines a set of abstract classes and interfaces called *extension points*.
Any plugin can contribute an implementation by annotating it with `@Extension`:

```java
@Extension
public class MyRootAction implements RootAction { ... }
```

Jenkins uses the `ServiceLoader` + annotation scanning at startup to discover all
`@Extension`-annotated classes and register them automatically. No manual wiring required.

Key extension points used in this repo:

| Extension Point | What it does |
|-----------------|-------------|
| `RootAction` | Adds a URL under Jenkins root (e.g. `/copilot-chat/`) and an optional sidebar link |
| `PageDecorator` | Injects HTML into every page (header/footer) — used to add the chat widget |
| `GlobalConfiguration` | Adds a section to *Manage Jenkins → System* with persistent settings |
| `UserProperty` | Attaches data to each Jenkins user account |

---

## 🌐 Stapler — The MVC Framework

Jenkins uses **Stapler** as its web framework (instead of Spring MVC or Jakarta EE). Stapler is a URL-binding framework that maps HTTP requests to Java objects by convention:

```
GET /copilot-chat/startLogin
     ↓
CopilotChatRootAction.doStartLogin()
```

Rules:
- A public method named `doXxx()` handles `GET /xxx`.
- A method that accepts `StaplerRequest2` / `StaplerResponse2` handles `POST`.
- Methods can return `HttpResponse` objects or write directly to the response.
- `@QueryParameter` injects URL query parameters as method arguments.

This means you add a new endpoint simply by adding a method — no routing configuration files.

---

## 🎨 Jelly Templates

Views in Jenkins plugins are written in **Apache Jelly** — an XML-based templating language.
Jelly files live in `src/main/resources/` and are resolved by Stapler based on the class hierarchy:

```
src/main/resources/
└── io/jenkins/plugins/copilotchat/
    ├── CopilotChatConfiguration/
    │   └── config.jelly          ← rendered in Manage Jenkins → System
    ├── CopilotChatPageDecorator/
    │   └── footer.jelly          ← injected into every page footer
    └── CopilotChatRootAction/
        └── index.jelly           ← rendered at /copilot-chat/
```

A Jelly template for `MyClass` must be at `io/my/package/MyClass/view.jelly`.
Stapler finds the right template automatically.

---

## ⚙️ GlobalConfiguration — Persistent Settings

`GlobalConfiguration` is a Jenkins extension point that:
1. Registers a configuration panel in *Manage Jenkins → System*.
2. Persists fields to an XML file in `$JENKINS_HOME/` via XStream serialization.
3. Exposes a `get()` static method for other classes to read the settings.

```java
@Extension
public class MyConfig extends GlobalConfiguration {
    private String apiUrl;

    public MyConfig() { load(); }           // reads XML on startup

    @DataBoundSetter
    public void setApiUrl(String v) {
        this.apiUrl = v;
        save();                             // writes XML immediately
    }
}
```

`@DataBoundSetter` tells Stapler to bind the form field with the same name to this setter when the user saves the configuration form.

---

## 👤 UserProperty — Per-User Data

`UserProperty` attaches arbitrary data to Jenkins user accounts. Each user's data is persisted in `$JENKINS_HOME/users/<id>/config.xml`.

```java
public class MyProperty extends UserProperty {
    private final String value;
    // ...
}

// Store it:
user.addProperty(new MyProperty("hello"));
user.save();

// Read it back:
MyProperty p = user.getProperty(MyProperty.class);
```

Jenkins encrypts sensitive fields using `hudson.util.Secret`:
```java
private Secret token;                           // stored encrypted on disk
String plain = Secret.toString(token);          // decrypted at runtime
Secret wrapped = Secret.fromString(plainText);  // wrap before storing
```

---

## 🖼️ PageDecorator — Injecting UI into Every Page

`PageDecorator` lets a plugin add HTML to the header or footer of **every** Jenkins page.
The decoration happens through a Jelly template at `ClassName/footer.jelly` (or `header.jelly`):

```java
@Extension
public class MyChatDecorator extends PageDecorator {}
// No Java code needed — the template does all the work
```

```xml
<!-- CopilotChatPageDecorator/footer.jelly -->
<j:jelly ...>
  <link rel="stylesheet" href="/plugin/copilot-chat/copilot-chat.css" />
  <script src="/plugin/copilot-chat/copilot-chat.js"></script>
  <div id="copilot-chat-widget"></div>
</j:jelly>
```

---

## 🔐 Permissions

Jenkins has a fine-grained permission system. Every action should call:

```java
Jenkins.get().checkPermission(Jenkins.READ);
```

This throws an `AccessDeniedException` if the current user doesn't have the required permission, which Stapler automatically converts into an HTTP 403 response.

---

## 🗂️ Static Assets (webapp/)

Files placed in `src/main/webapp/` are served directly by Jenkins at:
```
/plugin/<plugin-short-name>/<path>
```

For example, `src/main/webapp/copilot-chat.js` is served at `/plugin/copilot-chat/copilot-chat.js`.

---

## 📖 Further Reading

- [Jenkins Plugin Developer Guide](https://www.jenkins.io/doc/developer/)
- [Stapler Documentation](http://stapler.kohsuke.org/)
- [Jenkins Extension Points Index](https://www.jenkins.io/doc/developer/extensions/)

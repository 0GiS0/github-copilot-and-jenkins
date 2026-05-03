# 🤖 Copilot Chat Plugin — Cómo funciona por dentro

🇬🇧 [Read in English](copilot-chat-plugin-internals.en.md)

Este documento explica la arquitectura del plugin **Copilot Chat** para Jenkins y cómo cada
pieza del sistema de plugins de Jenkins (descrita en [how-jenkins-plugins-work.md](./how-jenkins-plugins-work.md))
se usó para construirlo.

---

## 🗺️ Visión General de la Arquitectura

```
Navegador (widget de chat)
   │  HTTP / Server-Sent Events (SSE)
   ▼
CopilotChatRootAction        ← Stapler RootAction  (endpoints REST)
   │                            + CopilotChatPageDecorator (inyecta JS/CSS)
   │
   ├── DeviceFlowAuthService  ← GitHub OAuth 2.0 Device Flow
   │      └── GitHubTokenStore  ← persiste el token como UserProperty de Jenkins
   │
   └── CopilotChatSessionService  ← gestiona las sesiones de IA por usuario
          └── CopilotClientFactory  ← crea clientes del SDK de Copilot
                 └── CopilotClient (SDK)
                        └── CopilotSession (SDK)
                               ├── Servidor MCP de Jenkins  ← llama a herramientas de la API de Jenkins
                               └── Servidor MCP de GitHub   ← llama a herramientas de la API de GitHub
```

---

## 🧩 Cómo se usó cada bloque de Jenkins

### `RootAction` → API REST + Enlace en la Barra Lateral

`CopilotChatRootAction` implementa `RootAction`, lo que nos da:
- Un espacio de nombres de URL en `/copilot-chat/`
- Un enlace en la barra lateral con un icono personalizado (símbolo SVG registrado en `webapp/images/symbols/`)
- Métodos públicos `doXxx()` que Stapler mapea automáticamente a endpoints HTTP

```
GET  /copilot-chat/startLogin   → doStartLogin()
GET  /copilot-chat/pollLogin    → doPollLogin(loginId)
GET  /copilot-chat/authStatus   → doAuthStatus()
GET  /copilot-chat/logout       → doLogout()
GET  /copilot-chat/models       → doModels()
POST /copilot-chat/sendMessage  → doSendMessage(request, response)
```

Sin tabla de rutas ni configuración de Spring — solo convenciones de nomenclatura de métodos.

---

### `PageDecorator` → Inyectar el Widget de Chat

`CopilotChatPageDecorator` es una clase vacía que extiende `PageDecorator`.
El trabajo real ocurre en `CopilotChatPageDecorator/footer.jelly`, que Jenkins
renderiza al final de **todas las páginas**. La plantilla Jelly:

1. Carga `copilot-chat.css` y `copilot-chat.js` desde `webapp/`.
2. Renderiza el `<div>` del widget de chat al que se enlaza el JavaScript.

Resultado: la burbuja de chat aparece en todas las páginas de Jenkins sin modificar ningún código del núcleo.

---

### `GlobalConfiguration` → Ajustes del Plugin

`CopilotChatConfiguration` almacena todos los ajustes del plugin en `$JENKINS_HOME/copilotChatConfiguration.xml`:

| Campo | Propósito |
|-------|-----------|
| `clientId` | Client ID de la OAuth App de GitHub para el Device Flow |
| `cliUrl` | URL del servidor remoto Copilot CLI HTTP |
| `cliPath` | Ruta a un binario local de Copilot CLI (alternativa) |
| `defaultModel` | Modelo de IA a usar (p. ej. `gpt-5.4`) |
| `availableTools` | Herramientas MCP separadas por comas que la IA puede llamar |
| `requestTimeoutSeconds` | Timeout de streaming |
| `jenkinsMcpUrl/Username/Token` | Credenciales para el servidor MCP de Jenkins |
| `githubMcpUrl/Token` | Credenciales para el servidor MCP de GitHub |

La plantilla `config.jelly` renderiza todos estos campos como un formulario en *Administrar Jenkins → Sistema*.
Los métodos `@DataBoundSetter` se llaman automáticamente cuando el usuario guarda el formulario.

---

### `UserProperty` → Almacenar Tokens de GitHub por Usuario

`CopilotTokenUserProperty` almacena tres valores por usuario de Jenkins:
- El **token de acceso** OAuth de GitHub (cifrado con `Secret` de Jenkins)
- El **login** de GitHub (cadena de nombre de usuario)
- El **ID de usuario** numérico de GitHub

`GitHubTokenStore` actúa como un DAO sobre esto:
```java
tokenStore.save(user, accessToken, login, id);   // tras el éxito del OAuth
tokenStore.getToken(user);                         // antes de cada llamada a la API
tokenStore.delete(user);                           // al cerrar sesión
```

Los tokens se cifran en reposo — Jenkins usa AES con una clave maestra almacenada en `$JENKINS_HOME/secrets/`.

---

## 🔑 GitHub OAuth 2.0 Device Flow

El plugin usa el **Device Authorization Grant** (RFC 8628) — el flujo OAuth diseñado para
dispositivos que no pueden abrir un navegador. Esto es ideal aquí porque el propio servidor Jenkins
inicia el flujo en nombre del navegador.

```
Navegador          Servidor Jenkins        GitHub
   │                    │                    │
   │── Click Login ────►│                    │
   │                    │── POST /device/code►│
   │                    │◄── {userCode, uri} ─│
   │◄── {loginId,      │                    │
   │     userCode,      │                    │
   │     verificationUri}                    │
   │                    │                    │
   │  (el usuario visita verificationUri y escribe el userCode en GitHub)
   │                    │                    │
   │── Poll cada 5s ───►│── POST /token ─────►│
   │                    │◄── authorization_pending
   │── Poll ────────────►│── POST /token ─────►│
   │                    │◄── {access_token} ──│
   │◄── {authenticated} │                    │
   │    login, id       │── GET /user ────────►│
   │                    │◄── {login, id} ─────│
   │                    │                    │
   │                    │ guarda token en UserProperty
```

Detalle de implementación clave: `pendingLogins` es un `ConcurrentHashMap` que almacena
el estado de los logins en curso entre las llamadas de inicio y poll. Está indexado por un UUID aleatorio
(`loginId`) para que múltiples usuarios puedan iniciar sesión de forma concurrente.

---

## 💬 Gestión de Sesiones (`CopilotChatSessionService`)

Cada usuario de Jenkins tiene una `UserChatSession` de larga duración que sobrevive a múltiples
turnos de chat (manteniendo el historial de conversación):

```java
private record UserChatSession(
    CopilotClient client,    // conexión al CLI de Copilot
    CopilotSession session,  // conversación de IA con estado
    String model             // modelo que usa esta sesión
) {}
```

Las sesiones se cachean en un `ConcurrentHashMap<String, UserChatSession>` indexado por el ID de usuario de Jenkins.

**Pasos de creación de sesión:**
1. `CopilotClientFactory` crea un `CopilotClient` (apuntando al servidor CLI remoto o a un binario local).
2. `client.start()` inicia el proceso CLI / conecta al servidor remoto.
3. `client.createSession(config)` abre una conversación de IA con:
   - El modelo elegido
   - Streaming habilitado
   - Un mensaje de sistema describiendo el contexto de Jenkins y las herramientas disponibles
   - Registros de servidores MCP (Jenkins + opcionalmente GitHub)
4. Un `CountDownLatch` espera a que los servidores MCP terminen de cargar antes de permitir mensajes.

**Cambio de modelo:** si el usuario selecciona un modelo diferente, la sesión anterior se detiene y se crea una nueva de forma transparente.

---

## 🌐 Integración con Servidores MCP

Los servidores MCP (Model Context Protocol) exponen operaciones como **herramientas** que la IA puede llamar de forma autónoma.
Se registran dos servidores MCP por sesión:

### 🏗️ MCP de Jenkins (`jenkins`)
Siempre registrado. Expone herramientas para:
- Leer jobs, builds, logs, resultados de tests
- Lanzar builds, repetir pipelines
- Encontrar URLs SCM (Git) de los jobs

Autenticación: HTTP Basic (usuario + token de API codificado en Base64).

### 🐙 MCP de GitHub (`github`)
Se registra solo cuando está configurado. Expone herramientas para:
- Leer y editar archivos en repositorios
- Crear ramas y pull requests
- Buscar issues y código

Autenticación: HTTP Bearer token (Personal Access Token de GitHub).

Esta combinación permite a la IA responder preguntas como:
> *"¿Por qué falló el último build?"* → llama a `getBuildLog()`  
> *"Arregla el Jenkinsfile y abre un PR"* → llama a `getJobScm()` + `createBranch()` + `editFile()` + `createPullRequest()`

---

## 📡 Respuestas en Streaming (Server-Sent Events)

La respuesta de la IA se transmite token a token usando **Server-Sent Events (SSE)**.

### ¿Por qué SSE?
- Soporte nativo en el navegador (API `EventSource` o `fetch()` con streaming)
- Push unidireccional servidor→cliente sobre una conexión HTTP estándar
- Funciona con la pila HTTP existente de Jenkins sin necesidad de WebSockets

### Cómo funciona

`StreamingHttpResponse` establece las cabeceras de respuesta y mantiene la conexión abierta:
```
Content-Type: text/event-stream
Cache-Control: no-cache
X-Accel-Buffering: no   ← deshabilita el buffering de nginx
```

El SDK de Copilot dispara eventos según van llegando del modelo de IA. El plugin registra
cuatro listeners por turno de chat:

| Evento del SDK | Evento SSE enviado al navegador |
|----------------|--------------------------------|
| `AssistantReasoningEvent` | `{"type":"reasoning","content":"..."}` |
| `AssistantMessageDeltaEvent` | `{"type":"delta","content":"..."}` |
| `AssistantMessageEvent` | (fallback, si no hay deltas) |
| `SessionIdleEvent` | `{"type":"complete"}` |
| `SessionErrorEvent` | `{"type":"error","message":"..."}` |

El JavaScript en `copilot-chat.js` lee estos eventos y añade los deltas al widget de chat en tiempo real.

Cada listener de eventos se almacena como `Closeable` y se elimina tras completarse el turno (o al producirse un error)
para evitar fugas de memoria por suscripciones residuales.

---

## 🔌 Modos del Cliente SDK de Copilot

`CopilotClientFactory` admite dos modos de despliegue:

| Modo | Cuándo usarlo | Cómo funciona |
|------|--------------|---------------|
| **Servidor CLI remoto** | Docker / Kubernetes | Establece `cliUrl`. El SDK envía peticiones HTTP a un contenedor `copilot-cli` en ejecución. No se necesita token de GitHub en el lado de Jenkins. |
| **CLI local** | Desarrollo / metal desnudo | Deja `cliUrl` en blanco. El SDK lanza un proceso local `copilot` CLI usando el token de GitHub del usuario. Opcionalmente establece `cliPath` para un binario específico. |

La configuración recomendada (usada en el Dev Container) es el modo de servidor CLI remoto — el servicio `copilot-cli` en `docker-compose.yml` gestiona toda la autenticación de Copilot y el acceso a los modelos.

---

## 🛡️ Consideraciones de Seguridad

- **Cifrado de tokens**: los tokens de GitHub se almacenan como `hudson.util.Secret`, cifrados con AES usando la clave maestra de Jenkins.
- **Comprobación de permisos**: cada endpoint llama a `Jenkins.get().checkPermission(Jenkins.READ)` antes de procesar.
- **Aislamiento de usuarios**: las sesiones y los tokens están indexados por ID de usuario de Jenkins — un usuario no puede acceder a la sesión o token de otro.
- **Sin exposición de tokens**: `CopilotClientOptions.setUseLoggedInUser(false)` evita que el SDK lea tokens de la configuración de Git del sistema.

---

## 📖 Documentación Relacionada

- [Cómo funcionan los plugins de Jenkins](./how-jenkins-plugins-work.md)
- [Jenkins Plugin Developer Guide](https://www.jenkins.io/doc/developer/)
- [GitHub Device Flow (RFC 8628)](https://datatracker.ietf.org/doc/html/rfc8628)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Especificación Server-Sent Events](https://html.spec.whatwg.org/multipage/server-sent-events.html)

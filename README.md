# 🤖 GitHub Copilot + Jenkins: CLI Integration y Chat Plugin

<div align="center">

[![YouTube Channel Subscribers](https://img.shields.io/youtube/channel/subscribers/UC140iBrEZbOtvxWsJ-Tb0lQ?style=for-the-badge&logo=youtube&logoColor=white&color=red)](https://www.youtube.com/c/GiselaTorres?sub_confirmation=1) [![GitHub followers](https://img.shields.io/github/followers/0GiS0?style=for-the-badge&logo=github&logoColor=white)](https://github.com/0GiS0) [![LinkedIn Follow](https://img.shields.io/badge/LinkedIn-Follow-blue?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/giselatorresbuitrago/) [![X Follow](https://img.shields.io/badge/X-Follow-black?style=for-the-badge&logo=x&logoColor=white)](https://twitter.com/0GiS0)

</div>

---

¡Hola developer 👋🏻! Este repositorio muestra cómo integrar **GitHub Copilot** con **Jenkins** de dos formas: usando la **GitHub Copilot CLI** en tus pipelines para automatizar code reviews, generación de documentación y análisis de código; y a través de un **plugin de Jenkins** que añade un chat interactivo con GitHub Copilot directamente en la interfaz de Jenkins.

<a href="https://youtu.be/VIDEO_ID">
 <img src="https://img.youtube.com/vi/VIDEO_ID/maxresdefault.jpg" alt="GitHub Copilot + Jenkins: CLI Integration y Chat Plugin" width="100%" />
</a>

---

## 📑 Tabla de Contenidos

- [✨ Características](#-características)
- [🛠️ Tecnologías](#️-tecnologías-utilizadas)
- [📋 Requisitos Previos](#-requisitos-previos)
- [🚀 Instalación](#-instalación)
- [💻 Uso](#-uso)
- [📁 Estructura del Proyecto](#-estructura-del-proyecto)
- [🔧 Pipelines Disponibles](#-pipelines-disponibles)
- [🔌 Plugin Copilot Chat para Jenkins](#-plugin-copilot-chat-para-jenkins)
- [🔒 Seguridad](#-seguridad)
- [🐛 Troubleshooting](#-troubleshooting)
- [📚 Recursos](#-recursos)
- [🌐 Sígueme](#-sígueme-en-mis-redes-sociales)

---

## ✨ Características

- 🤖 **GitHub Copilot CLI en pipelines** — Ejecuta análisis de código con IA directamente desde Jenkins
- 💬 **Plugin Copilot Chat** — Chat interactivo con GitHub Copilot integrado en la UI de Jenkins
- 🔐 **Autenticación OAuth Device Flow** — Login seguro con GitHub sin exponer credenciales
- 📝 **Code Review automático** — Revisiones de código generadas por IA en cada pipeline
- 📚 **Generación de documentación** — READMEs y docs de API creados automáticamente con Copilot
- 🔍 **Análisis de código** — Métricas de complejidad, sugerencias de seguridad y mejoras
- 🐳 **Dev Container preconfigurado** — Entorno listo para usar con Jenkins y todas las dependencias
- ⚙️ **Jenkins as Code (JCasC)** — Configuración automática y reproducible de Jenkins
- 🔒 **Gestión segura de credenciales** — Tokens de GitHub protegidos mediante Jenkins Credentials

---

## 🛠️ Tecnologías Utilizadas

- **[Jenkins](https://www.jenkins.io/)** — Servidor de integración y entrega continua (CI/CD)
- **[GitHub Copilot CLI](https://docs.github.com/en/copilot/github-copilot-in-the-cli)** — Binario `copilot` para análisis de código con IA en terminal
- **[GitHub Copilot SDK for Java](https://github.com/github/copilot-sdk-java)** — SDK oficial para integrar Copilot en aplicaciones Java
- **[Node.js / TypeScript](https://www.typescriptlang.org/)** — Proyecto de ejemplo para demostrar los pipelines
- **[Java 17](https://openjdk.org/)** — Lenguaje principal del plugin de Jenkins
- **[Maven](https://maven.apache.org/)** — Gestión de dependencias y build del plugin
- **[Docker / Dev Containers](https://containers.dev/)** — Entorno de desarrollo reproducible
- **[Jenkins Configuration as Code (JCasC)](https://www.jenkins.io/projects/jcasc/)** — Configuración declarativa de Jenkins
- **[Jest](https://jestjs.io/)** — Testing del proyecto TypeScript de ejemplo

---

## 📋 Requisitos Previos

- **[Docker Desktop](https://www.docker.com/products/docker-desktop)** instalado y en ejecución (mínimo 4 GB de RAM asignados)
- **[Visual Studio Code](https://code.visualstudio.com/)** con la extensión [Dev Containers](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)
- **Token de GitHub** (`GH_TOKEN`) con los siguientes permisos:
  - *Fine-grained PAT*: acceso al repositorio de prueba + permiso `Contents: Read` + permiso `Copilot Requests`
  - *Classic PAT*: scope `repo` + acceso a GitHub Copilot en tu cuenta u organización
- **Suscripción activa a GitHub Copilot** (individual o de organización)
- **Java 17+** y **Maven 3.9+** si quieres compilar el plugin fuera del Dev Container

---

## 🚀 Instalación

### Paso 1: Clonar el repositorio

```bash
git clone https://github.com/0GiS0/github-copilot-and-jenkins.git
cd github-copilot-and-jenkins
```

### Paso 2: Configurar el token de GitHub

```bash
# macOS / Linux
export GH_TOKEN="tu_token_aqui"

# Windows PowerShell
$env:GH_TOKEN="tu_token_aqui"
```

> **Nota:** El Dev Container sincroniza automáticamente esta variable con las credenciales de Jenkins al arrancar. Si el token cambia con el contenedor ya abierto, ejecuta:
> ```bash
> scripts/sync-jenkins-gh-token.sh
> ```

### Paso 3: Abrir el Dev Container

1. Abre VS Code en el directorio del repositorio:
   ```bash
   code .
   ```
2. Cuando VS Code detecte el Dev Container, haz clic en **"Reopen in Container"**
3. Espera a que Docker construya y levante los contenedores (~5 minutos la primera vez)

### Paso 4: Acceder a Jenkins

Una vez levantado el Dev Container, abre tu navegador en:

| Campo     | Valor                     |
|-----------|---------------------------|
| URL       | http://localhost:8081     |
| Usuario   | `admin`                   |
| Contraseña| `admin`                   |

---

## 💻 Uso

### Ejecutar un pipeline de Copilot CLI

En Jenkins, navega a uno de los pipelines disponibles bajo `copilot-demos/` y haz clic en **"Build with Parameters"**:

```
Jenkins > copilot-demos > code-review > Build with Parameters
```

### Usar el Chat de Copilot en la UI de Jenkins

Una vez instalado el plugin `copilot-chat`:

1. Aparece un **botón flotante** con el logo de GitHub Copilot en la esquina de la interfaz de Jenkins
2. Haz clic para abrir el panel de chat
3. Pulsa **"Sign in with GitHub"** y sigue el flujo de autenticación Device Flow:
   - Se mostrará un código de dispositivo y una URL de verificación
   - Introduce el código en GitHub para autorizar el acceso
4. ¡Listo! Ya puedes **chatear con GitHub Copilot** desde dentro de Jenkins

```
Jenkins > (botón flotante Copilot Chat) > Sign in with GitHub > Escribe tu pregunta
```

### Ejemplo: Ejecutar el pipeline principal con todos los demos

```groovy
// Opciones del pipeline principal (Jenkinsfile)
DEMO_TYPE  → ALL | CODE_REVIEW | DOCS_GENERATOR | CODE_ANALYSIS
RUN_TESTS  → true | false
INSTALL_DEPS → true | false
```

### Ejemplo: Usar Copilot CLI en un stage personalizado

```groovy
stage('Análisis con Copilot') {
    steps {
        withCredentials([string(credentialsId: 'gh-token', variable: 'COPILOT_GITHUB_TOKEN')]) {
            sh '''
                copilot --autopilot --yolo --max-autopilot-continues 3 \
                  --prompt "Review src/ and return concise Markdown recommendations"
            '''
        }
    }
}
```

---

## 📁 Estructura del Proyecto

```
github-copilot-and-jenkins/
├── .devcontainer/
│   ├── devcontainer.json           # Configuración del Dev Container
│   ├── docker-compose.yml          # Servicios Docker (Jenkins + devcontainer)
│   ├── Dockerfile.devcontainer     # Imagen del entorno de desarrollo
│   ├── Dockerfile.jenkins          # Imagen de Jenkins personalizada
│   └── jenkins-config/
│       ├── casc.yaml               # Jenkins Configuration as Code
│       └── plugins.txt             # Plugins de Jenkins a instalar
├── jenkins-copilot-chat-plugin/    # 🔌 Plugin de Jenkins para chat con Copilot
│   └── src/main/
│       ├── java/io/jenkins/plugins/copilotchat/
│       │   ├── CopilotChatRootAction.java      # Endpoint REST del chat
│       │   ├── CopilotChatConfiguration.java   # Configuración del plugin
│       │   ├── CopilotChatSessionService.java  # Gestión de sesiones de chat
│       │   ├── DeviceFlowAuthService.java      # Autenticación OAuth Device Flow
│       │   └── CopilotChatPageDecorator.java   # Inyección del widget en la UI
│       ├── resources/                          # Templates Jelly (UI)
│       └── webapp/
│           ├── copilot-chat.js                 # Lógica del widget de chat
│           ├── copilot-chat.css                # Estilos del widget
│           └── images/                         # Logos e iconos
├── pipelines/
│   ├── code-review.jenkinsfile     # Pipeline de code review automático
│   ├── docs-generator.jenkinsfile  # Pipeline de generación de documentación
│   └── code-analysis.jenkinsfile   # Pipeline de análisis de código
├── scripts/
│   ├── install-copilot-cli.sh      # Script de instalación de Copilot CLI
│   └── sync-jenkins-gh-token.sh    # Sincronización del token con Jenkins
├── src/                            # Proyecto Node.js/TypeScript de ejemplo
│   ├── index.ts
│   ├── utils.ts
│   └── utils.test.ts
├── Jenkinsfile                     # Pipeline principal orquestador
├── package.json
├── tsconfig.json
└── README.md
```

---

## 🔧 Pipelines Disponibles

### 📝 Code Review (`copilot-demos/code-review`)

Ejecuta una revisión automática del código usando Copilot CLI:

- Analiza cada archivo TypeScript del proyecto
- Genera sugerencias de mejoras y buenas prácticas
- Crea un reporte en Markdown como artefacto de Jenkins

### 📚 Documentation Generator (`copilot-demos/docs-generator`)

Genera documentación automáticamente con IA:

- README con instrucciones de uso
- Documentación de API y funciones
- Explicación detallada del código

### 🔍 Code Analysis (`copilot-demos/code-analysis`)

Analiza en profundidad la calidad del código:

- Métricas de complejidad ciclomática
- Sugerencias de seguridad y vulnerabilidades
- Recomendaciones de refactoring
- Reporte HTML publicado en Jenkins como **Copilot Code Analysis Report**
- Artefactos descargables: `analysis/report.html` y `analysis/report.md`

### 🚀 Main Pipeline (`Jenkinsfile`)

Pipeline orquestador que ejecuta todos los demos de forma configurable:

```
Jenkins > main-pipeline > Build with Parameters
```

| Parámetro     | Opciones                                            |
|---------------|-----------------------------------------------------|
| `DEMO_TYPE`   | `ALL`, `CODE_REVIEW`, `DOCS_GENERATOR`, `CODE_ANALYSIS` |
| `RUN_TESTS`   | `true` / `false`                                    |
| `INSTALL_DEPS`| `true` / `false`                                    |

---

## 🔌 Plugin Copilot Chat para Jenkins

El directorio `jenkins-copilot-chat-plugin/` contiene un **plugin de Jenkins** que integra un widget de chat con GitHub Copilot directamente en la interfaz web de Jenkins.

### ¿Qué hace?

- Añade un **botón flotante** con el logo de GitHub Copilot en todas las páginas de Jenkins
- Abre un **panel de chat** lateral donde puedes conversar con Copilot sobre tus builds, pipelines o código
- Usa **OAuth Device Flow** para autenticar tu cuenta de GitHub de forma segura
- Gestiona **sesiones de chat individuales** por usuario de Jenkins
- Permite configurar el **modelo de IA** (por defecto `gpt-4.1`), las herramientas disponibles y el timeout

### Configuración del plugin

Una vez instalado el `.hpi`, accede a la configuración global de Jenkins:

```
Manage Jenkins > System > Copilot Chat
```

| Campo                  | Descripción                                      | Valor por defecto   |
|------------------------|--------------------------------------------------|---------------------|
| Client ID              | ID de la GitHub App o OAuth App                  | —                   |
| CLI Path               | Ruta al binario `copilot` (opcional)             | —                   |
| Default Model          | Modelo de IA a utilizar                          | `gpt-4.1`           |
| Available Tools        | Herramientas habilitadas para Copilot            | `read_file,search_code,list_dir` |
| Request Timeout (s)    | Tiempo máximo de espera por respuesta            | `120`               |

### Compilar el plugin

```bash
cd jenkins-copilot-chat-plugin
mvn clean package -DskipTests
# El archivo .hpi se genera en: target/copilot-chat.hpi
```

### Instalar manualmente el plugin

```
Manage Jenkins > Manage Plugins > Advanced > Upload Plugin > selecciona copilot-chat.hpi
```

---

## 🔒 Seguridad

> ⚠️ **Importante**: Nunca subas tu `GH_TOKEN` al repositorio.

Este proyecto gestiona las credenciales de forma segura:

- **Jenkins Credentials** — El token se almacena cifrado en Jenkins, nunca en texto plano
- **Variable de entorno en Dev Container** — El token se pasa desde el host, no se guarda en el contenedor
- **Credential Binding en pipelines** — Los stages acceden al token solo cuando lo necesitan

Jenkins crea automáticamente dos credenciales a partir de `GH_TOKEN`:

| Credencial       | Tipo            | Uso                                              |
|------------------|-----------------|--------------------------------------------------|
| `gh-token`       | Secret text     | Ejecutar el binario `copilot` en los pipelines   |
| `github-token`   | Username/Password | Clonar el repositorio privado desde Jenkins    |

---

## 🐛 Troubleshooting

### El Dev Container no levanta

1. Verifica que Docker Desktop esté corriendo
2. Comprueba que tienes suficiente memoria asignada a Docker (mínimo 4 GB)
3. Intenta reconstruir: `Ctrl+Shift+P` > "Dev Containers: Rebuild Container"

### Jenkins no reconoce el token

1. Verifica que `GH_TOKEN` esté definido **antes** de abrir VS Code
2. Ejecuta `scripts/sync-jenkins-gh-token.sh` desde la terminal integrada
3. Reinicia el Dev Container si `GH_TOKEN` se definió después de abrir el contenedor
4. Comprueba las credenciales en: **Manage Jenkins > Credentials**

> Para repos privados: usa scope `repo` en classic PAT o permiso `Contents: Read` en fine-grained PAT.

### Copilot CLI no responde

1. Verifica que tu token tenga el permiso `Copilot Requests`
2. Comprueba tu suscripción activa a GitHub Copilot
3. Ejecuta `copilot --version` para verificar que el binario está instalado

### El widget de chat no aparece en Jenkins

1. Verifica que el plugin `.hpi` esté instalado correctamente
2. Comprueba que el **Client ID** esté configurado en: **Manage Jenkins > System > Copilot Chat**
3. Reinicia Jenkins tras la instalación del plugin

---

## 📚 Recursos

- [GitHub Copilot CLI Documentation](https://docs.github.com/en/copilot/github-copilot-in-the-cli)
- [GitHub Copilot SDK for Java](https://github.com/github/copilot-sdk-java)
- [Jenkins Configuration as Code (JCasC)](https://www.jenkins.io/projects/jcasc/)
- [Dev Containers Specification](https://containers.dev/)
- [Jenkins Plugin Development](https://www.jenkins.io/doc/developer/plugin-development/)
- [OAuth Device Flow — GitHub Docs](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow)

---

## 🌐 Sígueme en Mis Redes Sociales

Si te ha gustado este proyecto y quieres ver más contenido como este, no olvides suscribirte a mi canal de YouTube y seguirme en mis redes sociales:

<div align="center">

[![YouTube Channel Subscribers](https://img.shields.io/youtube/channel/subscribers/UC140iBrEZbOtvxWsJ-Tb0lQ?style=for-the-badge&logo=youtube&logoColor=white&color=red)](https://www.youtube.com/c/GiselaTorres?sub_confirmation=1)
[![GitHub followers](https://img.shields.io/github/followers/0GiS0?style=for-the-badge&logo=github&logoColor=white)](https://github.com/0GiS0)
[![LinkedIn Follow](https://img.shields.io/badge/LinkedIn-Sígueme-blue?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/giselatorresbuitrago/)
[![X Follow](https://img.shields.io/badge/X-Sígueme-black?style=for-the-badge&logo=x&logoColor=white)](https://twitter.com/0GiS0)

</div>

# 🤖 GitHub Copilot + Jenkins Integration

[![CI](https://github.com/0GiS0/github-copilot-and-jenkins/actions/workflows/ci.yml/badge.svg)](https://github.com/0GiS0/github-copilot-and-jenkins/actions/workflows/ci.yml)
[![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white)](https://www.jenkins.io/)
[![GitHub Copilot](https://img.shields.io/badge/GitHub%20Copilot-000000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/features/copilot)
[![DevContainer](https://img.shields.io/badge/DevContainer-007ACC?style=for-the-badge&logo=visual-studio-code&logoColor=white)](https://containers.dev/)
[![Java](https://img.shields.io/badge/Java-17-ED8936?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/)

---

¡Hola developer 👋🏻! Bienvenido a este proyecto que demuestra cómo integrar **GitHub Copilot** directamente en **Jenkins** usando un **plugin personalizado** y **Dev Containers** para una configuración reproducible y lista para usar.

<a href="https://youtu.be/TU_CODIGO_AQUI">
 <img src="https://img.youtube.com/vi/TU_CODIGO_AQUI/maxresdefault.jpg" alt="Cómo integrar Jenkins con GitHub Copilot" width="100%" />
</a>

---

## Sígueme en mis redes sociales

[![YouTube Channel Subscribers](https://img.shields.io/youtube/channel/subscribers/UCHpN7VxVA7trIjc_2S2CvEQ?style=social)](https://www.youtube.com/@returngis?sub_confirmation=1)
[![LinkedIn Follow](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/giselatorresbuitrago/)
[![X Follow](https://img.shields.io/badge/X-000000?style=for-the-badge&logo=x&logoColor=white)](https://twitter.com/0GiS0)
[![TikTok](https://img.shields.io/badge/TikTok-000000?style=for-the-badge&logo=tiktok&logoColor=white)](https://www.tiktok.com/@returngis)
[![Blog](https://img.shields.io/badge/Blog-FF5722?style=for-the-badge&logo=blogger&logoColor=white)](https://www.returngis.net)

---

## 📑 Tabla de Contenidos

- [Características](#características)
- [Qué incluye este proyecto](#qué-incluye-este-proyecto)
- [Tecnologías](#️-tecnologías-utilizadas)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación](#-instalación)
- [Configuración](#-configuración)
- [Uso](#-uso)
- [Pipelines de Demostración](#-pipelines-de-demostración)
- [Ejemplos de Copilot CLI](#-ejemplos-de-copilot-cli-en-groovy)
- [Seguridad](#-seguridad)
- [Troubleshooting](#-troubleshooting)
- [Recursos](#-recursos-y-documentación)
- [Contribuir](#-contribuir)

---

## ✨ Características

- **Plugin Copilot Chat**: Widget de chat flotante integrado en Jenkins para interactuar con GitHub Copilot
- **Autenticación OAuth**: Device Flow para conectarse a GitHub de forma segura (igual que `gh auth login`)
- **Streaming en tiempo real**: Respuestas SSE para una experiencia fluida
- **Dev Container preconfigurado**: Jenkins + todas las herramientas necesarias, lista en 5 minutos
- **Jenkins Configuration as Code (JCasC)**: Configuración automática al iniciar
- **3 Pipelines de demostración**: Code Review, Documentación y Análisis de código
- **Soporte para herramientas MCP**: Integración con Jenkins MCP y GitHub MCP
- **Selector de modelos**: Elige el modelo de IA que prefieras (GPT-4, etc.)
- **Proyecto Node.js de ejemplo**: TypeScript + Jest para experimentar

## 🎯 ¿Qué incluye este proyecto?

- **Dev Container** preconfigurado con Jenkins, Docker y todas las herramientas
- **Plugin Copilot Chat** personalizado para añadir chat de IA a Jenkins
- **Jenkins Configuration as Code (JCasC)** para setup automático
- **Plugin MCP Server** para conectar GitHub Copilot Chat con Jenkins vía Model Context Protocol
- **3 pipelines de demostración** usando Copilot CLI:
  - 📝 **Code Review**: Revisión automática del código
  - 📚 **Documentación**: Generación de README y documentación
  - 🔍 **Análisis**: Análisis de código y sugerencias
- **Proyecto Node.js/TypeScript** de ejemplo para probar

## 🛠️ Tecnologías Utilizadas

**Backend & DevOps:**
- **Jenkins 2.479.3**: Orquestación y CI/CD
- **Java 17**: Plugin Copilot Chat
- **Maven**: Build del plugin
- **Docker & Docker Compose**: Containerización

**Frontend:**
- **Node.js 20+**: Proyecto de ejemplo
- **TypeScript 5.3**: Tipado estático
- **Jest**: Testing unitario

**GitHub & IA:**
- **GitHub Copilot CLI**: Análisis y generación de código
- **GitHub Copilot SDK Java**: SDK para el plugin (v0.3.0-java-preview.1)
- **GitHub MCP**: Integración con herramientas MCP
- **Jenkins MCP Plugin**: Model Context Protocol

**Librerías clave:**
- **Jackson 2.17.2**: Serialización JSON
- **Streaming SSE**: Respuestas en tiempo real
- **OAuth Device Flow**: Autenticación segura

## 🚀 Instalación

### Paso 1: Configurar el Token de GitHub

Necesitas un token de GitHub con acceso a Copilot y permisos sobre este repositorio:

1. Ve a **[GitHub Settings > Developer Settings > Personal Access Tokens](https://github.com/settings/tokens)**
2. Crea un nuevo token con estos permisos:
   - **Fine-grained PAT** (recomendado):
     - Acceso al repositorio
     - `Contents: Read` (lectura de código)
     - `Copilot Requests` (para Copilot CLI)
   - O **Classic PAT**: 
     - Scope `repo` (acceso completo)
3. Copia el token
4. Configura la variable de entorno en tu terminal:

   ```bash
   # macOS/Linux
   export GH_TOKEN="tu_token_aqui"
   
   # Windows PowerShell
   $env:GH_TOKEN="tu_token_aqui"
   ```

> ⚠️ El Dev Container usará esta variable para sincronizar el token con Jenkins al iniciar. Si ya tienes `gh auth login` en tu máquina, el Dev Container también puede usar ese token.

### Paso 2: Clonar el Repositorio

```bash
# Clona el repositorio
git clone https://github.com/0GiS0/github-copilot-and-jenkins.git
cd github-copilot-and-jenkins

# Abre VS Code en esta carpeta
code .
```

### Paso 3: Abrir en Dev Container

1. VS Code detectará automáticamente la configuración del Dev Container
2. Haz clic en el botón **"Reopen in Container"** (abajo a la derecha)
3. Espera a que Docker construya la imagen (5-10 minutos la primera vez)
4. Una vez completado, el Dev Container estará listo

**Indicadores de que todo está bien:**
- ✅ Ves `[devcontainer]` en la terminal de VS Code
- ✅ Jenkins está disponible en `http://localhost:8081`
- ✅ El plugin Copilot Chat está instalado

### Paso 4: Acceder a Jenkins

Una vez levantado el Dev Container:

- **URL**: http://localhost:8081
- **Usuario**: `admin`
- **Contraseña**: `admin`

> 🔐 Si quieres cambiar la contraseña, edita `.devcontainer/jenkins-config/casc.yaml` antes de abrir el Dev Container

### Paso 5: Sincronizar Token si es Necesario

Si el token cambió después de abrir el Dev Container, sincronízalo ejecutando desde la terminal integrada:

```bash
./scripts/sync-jenkins-gh-token.sh
```

## ⚙️ Configuración

### 1️⃣ Configurar GitHub Copilot Chat + MCP Server de Jenkins

El Dev Container instala automáticamente el plugin [MCP Server](https://plugins.jenkins.io/mcp-server/), que expone Jenkins como un servidor [Model Context Protocol](https://modelcontextprotocol.io/). Esto permite a GitHub Copilot Chat consultar y operar Jenkins directamente desde el editor con lenguaje natural.

**Pasos:**

1. **Genera un API token de Jenkins**:
   - Ve a http://localhost:8081 con usuario `admin` y contraseña `admin`
   - Click en tu nombre (arriba a la derecha) → **Security** → **Add new token**
   - Nombre: `mcp-copilot`
   - Copia el token generado

2. **Codifica las credenciales en Base64**:
   ```bash
   echo -n "admin:TU_API_TOKEN" | base64
   ```

3. **Activa el servidor en VS Code**:
   - Paleta de comandos: `Cmd/Ctrl + Shift + P`
   - Escribe: `MCP: List Servers`
   - Selecciona `jenkins` → `Start Server`
   - Cuando pida credenciales, pega el valor base64

4. **Prueba con Copilot Chat**:
   - Abre Copilot Chat (`Ctrl+Shift+I` en VS Code)
   - Activa el modo **Agent** (@ symbol)
   - Prueba prompts como:
     - "Lista los jobs disponibles en Jenkins"
     - "Dame el estado del último build"
     - "Lanza el job code-review"

> **Info**: El MCP Server usa el endpoint stateless (`http://jenkins:8080/mcp-server/stateless`) para evitar problemas de resolución de DNS dentro del Dev Container.

### 2️⃣ Configurar Plugin Copilot Chat

El plugin **Copilot Chat** añade un widget de chat flotante a todas las páginas de Jenkins para interactuar con GitHub Copilot directamente desde Jenkins.

**Pasos:**

1. **Accede a la configuración global**:
   - Jenkins Dashboard → **Manage Jenkins** → **GitHub Copilot Chat**

2. **Rellena los campos**:
   - **Client ID**: Tu Client ID de OAuth App (deja vacío para usar el por defecto)
   - **CLI Path**: Ruta al binario `copilot` (por defecto: `/usr/local/bin/copilot`)
   - **Default Model**: `gpt-4` (o el modelo que prefieras)
   - **Request Timeout**: `30` segundos
   - **Jenkins MCP**: URL de tu servidor MCP de Jenkins (opcional)
   - **GitHub MCP**: Token para GitHub MCP (opcional)

3. **Haz clic en "Save"**

4. **El widget debería aparecer** en la esquina inferior derecha de cualquier página de Jenkins

> 🔧 Si no ves el widget, revisa la consola del navegador (`F12`) para ver si hay errores de carga.

**Autenticación del Plugin (Device Flow):**

La primera vez que uses el chat en Jenkins, se te pedirá que hagas login en GitHub:

1. Haz clic en **"Start Login"** en el widget
2. Se abrirá una pantalla con un código de dispositivo
3. Ve a GitHub.com y autoriza el acceso
4. El widget confirmará el login
5. Listo, ya puedes chatear con Copilot en Jenkins

## 📁 Estructura del Proyecto

```
.
├── .devcontainer/
│   ├── devcontainer.json        # Configuración del Dev Container
│   ├── docker-compose.yml       # Servicios Docker
│   ├── Dockerfile.devcontainer  # Imagen para desarrollo
│   ├── Dockerfile.jenkins       # Imagen de Jenkins personalizada
│   └── jenkins-config/
│       ├── casc.yaml            # Jenkins Configuration as Code
│       └── plugins.txt          # Plugins de Jenkins
├── jenkins-copilot-chat-plugin/ # Plugin Copilot Chat para Jenkins
│   ├── pom.xml                  # Maven configuration
│   ├── src/
│   │   ├── main/java/io/jenkins/plugins/copilotchat/
│   │   │   ├── CopilotChatPageDecorator.java      # Inyecta el widget
│   │   │   ├── CopilotChatConfiguration.java      # Configuración global
│   │   │   ├── DeviceFlowAuthService.java         # OAuth Device Flow
│   │   │   └── ... (otros componentes)
│   │   └── main/resources/
│   │       ├── index.jelly                        # Descripción del plugin
│   │       └── copilot-chat.{css,js}              # Frontend del widget
│   └── target/copilot-chat-0.2.0-SNAPSHOT.hpi    # Plugin compilado
├── src/                         # Proyecto Node.js de ejemplo
│   ├── index.ts
│   ├── utils.ts
│   └── utils.test.ts
├── pipelines/
│   ├── code-review.jenkinsfile
│   ├── docs-generator.jenkinsfile
│   └── code-analysis.jenkinsfile
├── scripts/
│   └── install-copilot-cli.sh
├── Jenkinsfile                  # Pipeline principal
├── package.json
└── README.md
```

## 🎮 Uso

### 🎯 Usando el Widget Copilot Chat en Jenkins

Una vez configurado, el widget aparecerá en la esquina inferior derecha de todas las páginas de Jenkins.

**Características:**
- 💬 Chat interactivo con GitHub Copilot
- 📝 Respuestas en Markdown con sintaxis resaltada
- 🔄 Streaming en tiempo real
- 📌 Redimensionable y maximizable
- 🛠️ Acceso a herramientas MCP (Jenkins + GitHub)

**Ejemplo de uso:**
1. Abre una página de Jenkins (Dashboard, Job, Build Log, etc.)
2. Haz clic en el widget de chat
3. Escribe un prompt, ej:
   - "Dame un resumen de lo que hace este job"
   - "Analiza los logs y dame las líneas críticas"
   - "¿Qué falta para mejorar esta pipeline?"

### 🚀 Ejecutando los Pipelines de Demostración

Los tres pipelines de demostración están preconfigurados en Jenkins:

1. Ve a **Jenkins Dashboard**
2. Busca la carpeta **`copilot-demos`**
3. Selecciona el pipeline que quieras ejecutar
4. Click en **"Build with Parameters"** (si está disponible)
5. Configura los parámetros si es necesario
6. Click en **"Build"**
7. Monitorea el progreso en la consola de construcción

## 🔧 Pipelines de Demostración

### 📝 Code Review (`copilot-demos/code-review`)

**Propósito:** Realizar revisión automática del código usando Copilot CLI.

**Qué hace:**
- Analiza todos los archivos TypeScript en `src/`
- Genera sugerencias de mejoras
- Identifica posibles bugs o anti-patrones
- Crea un reporte en Markdown

**Salida:**
- Reporte disponible como artefacto en Jenkins
- Impreso en los logs de la construcción

---

### 📚 Documentation Generator (`copilot-demos/docs-generator`)

**Propósito:** Generar documentación automáticamente usando Copilot.

**Qué hace:**
- Crea un README detallado con instrucciones
- Genera documentación de API
- Documenta cada función y su propósito
- Explica la arquitectura del proyecto

**Salida:**
- Archivos Markdown en `docs/`
- Guardados como artefactos

---

### 🔍 Code Analysis (`copilot-demos/code-analysis`)

**Propósito:** Análisis profundo de la calidad del código.

**Qué hace:**
- Métricas de complejidad
- Sugerencias de seguridad
- Recomendaciones de refactoring
- Genera reportes HTML e interactivos

**Salida:**
- Reporte HTML en `analysis/report.html`
- Reporte Markdown en `analysis/report.md`
- **Publicado en Jenkins** como "Copilot Code Analysis Report"

---

### 🎭 Main Pipeline

**Propósito:** Orquestador que ejecuta los demos según configuración.

**Parámetros:**
- `DEMO_TYPE`: Selecciona qué ejecutar
  - `ALL`: Todos los demos
  - `CODE_REVIEW`: Solo revisión
  - `DOCS_GENERATOR`: Solo documentación
  - `CODE_ANALYSIS`: Solo análisis
- `RUN_TESTS`: Ejecutar tests antes
- `INSTALL_DEPS`: Instalar npm antes

## 💡 Ejemplos de Copilot CLI en Groovy

Este proyecto usa el binario nuevo **`copilot`** (no `gh-copilot`).

### Ejemplo 1: Análisis con agentes

```groovy
stage('Copilot Agent Analysis') {
    steps {
        withCredentials([string(credentialsId: 'gh-token', variable: 'COPILOT_GITHUB_TOKEN')]) {
            sh '''
                copilot --prompt "Analyze this repository and summarize the main risks"
            '''
        }
    }
}
```

### Ejemplo 2: Ejecución en modo autopilot

```groovy
stage('Copilot Agent Report') {
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

### Ejemplo 3: Análisis de directorio específico

```groovy
stage('Analyze With Copilot') {
    steps {
        withCredentials([string(credentialsId: 'gh-token', variable: 'COPILOT_GITHUB_TOKEN')]) {
            sh '''
                copilot --prompt "Analyze the src/ directory and identify security issues" > analysis.txt
                cat analysis.txt
            '''
        }
    }
}
```

## 🔐 Seguridad

### Protección de credenciales

⚠️ **Nunca subas tu `GH_TOKEN` al repositorio.**

Este proyecto implementa múltiples capas de seguridad:

| Componente | Mecanismo | Descripción |
|-----------|-----------|-------------|
| **Dev Container** | Variable de entorno | Token se pasa desde tu máquina, no se almacena en el repo |
| **Jenkins** | Jenkins Credentials Store | Credenciales encriptadas en el fichero de configuración |
| **Pipelines** | `withCredentials{}` | Token se usa solo dentro del scope, no aparece en logs |
| **Git** | `.gitignore` | Archivos sensibles se ignoran automáticamente |

### Credenciales creadas automáticamente

Cuando inicia el Dev Container, se crean dos credenciales a partir de `GH_TOKEN`:

- **`gh-token`**: Token secreto para Copilot CLI
- **`github-token`**: Usuario/contraseña para Git (opcional, si el repo es privado)

### Buenas prácticas

✅ **Recomendado:**
- Usar fine-grained PAT con permisos específicos
- Regenerar el token regularmente
- Usar `gh auth login` en tu máquina como alternativa

❌ **Evitar:**
- Compartir tokens
- Almacenarlos en archivos sin encriptar
- Usar tokens con permisos excesivos

## 🐛 Troubleshooting

### ❌ El Dev Container no levanta

**Síntoma:** Error al reconstruir o conectar.

**Soluciones:**
1. ✅ Verifica que Docker Desktop esté **ejecutándose**
2. ✅ Comprueba memoria asignada a Docker: **mínimo 4GB** (preferible 8GB)
3. ✅ Intenta reconstruir: `Cmd/Ctrl + Shift + P` → "Dev Containers: Rebuild Container"
4. ✅ Revisa los logs: `docker logs <container_id>`

---

### ❌ Jenkins no inicia

**Síntoma:** `localhost:8081` no responde.

**Soluciones:**
1. ✅ Espera unos minutos, Jenkins tarda al iniciar (incluso 2-3 minutos)
2. ✅ Revisa los logs del contenedor: `docker logs jenkins`
3. ✅ Comprueba que el puerto 8081 no esté ocupado: `lsof -i :8081`
4. ✅ Si persiste, elimina la carpeta `jenkins_home` y reinicia

---

### ❌ Jenkins no reconoce el token de GitHub

**Síntoma:** Errores de autenticación en los pipelines.

**Soluciones:**
1. ✅ Verifica que `GH_TOKEN` esté definido **antes** de abrir VS Code
2. ✅ Ejecuta el script de sincronización: `./scripts/sync-jenkins-gh-token.sh`
3. ✅ Reinicia el Dev Container si definiste `GH_TOKEN` después
4. ✅ Ve a Jenkins → Manage Jenkins → Credentials para verificar

**Si el repo es privado:**
- ✅ El token debe ser válido para GitHub API
- ✅ Usar fine-grained PAT: permisos `Contents: Read` en el repo
- ✅ O usar classic PAT: scope `repo`

---

### ❌ Copilot CLI no responde

**Síntoma:** Timeouts o errores "connection refused" en los pipelines.

**Soluciones:**
1. ✅ Verifica que tu token tenga permiso **`Copilot Requests`**
2. ✅ Comprueba tu **suscripción a GitHub Copilot** activa
3. ✅ Prueba manualmente: `copilot --version`
4. ✅ Verifica conectividad: `curl -I https://api.github.com`

---

### ❌ El widget Copilot Chat no aparece en Jenkins

**Síntoma:** No ves el chat flotante en las páginas de Jenkins.

**Soluciones:**
1. ✅ Abre la consola del navegador: `F12` → "Console"
2. ✅ Busca errores como "Failed to load" o "403"
3. ✅ Verifica la configuración: Manage Jenkins → GitHub Copilot Chat
4. ✅ Intenta refrescar la página: `Ctrl+Shift+R` (hard refresh)
5. ✅ Comprueba que el plugin está instalado: Manage Jenkins → Plugins

---

### ❌ El MCP Server de Jenkins no responde en Copilot Chat

**Síntoma:** "Failed to connect to Jenkins MCP server"

**Soluciones:**
1. ✅ Verifica que el endpoint sea: `http://jenkins:8080/mcp-server/stateless` (desde dentro del Dev Container)
2. ✅ Prueba manualmente: `curl http://jenkins:8080/mcp-server/stateless`
3. ✅ Genera un nuevo API token y recodifica en Base64
4. ✅ Reinicia el servidor MCP en VS Code: `Cmd/Ctrl + Shift + P` → "MCP: List Servers"

## 🎓 Recursos y Documentación

- 📖 [GitHub Copilot CLI Documentation](https://docs.github.com/en/copilot/github-copilot-in-the-cli)
- 📖 [Jenkins Configuration as Code (JCasC)](https://www.jenkins.io/projects/jcasc/)
- 📖 [Dev Containers Specification](https://containers.dev/)
- 📖 [Model Context Protocol (MCP)](https://modelcontextprotocol.io/)
- 📖 [GitHub Copilot Java SDK](https://github.com/github/copilot-sdk-java)
- 📖 [Jenkins MCP Plugin](https://plugins.jenkins.io/mcp-server/)
- 📖 [Copilot Chat in VS Code](https://docs.github.com/en/copilot/github-copilot-in-the-cli)

## 📄 Licencia

MIT License - Siéntete libre de usar este proyecto como base para tus propias integraciones.

---

## 🙏 Agradecimientos

- Gracias a la comunidad de Jenkins por las herramientas increíbles
- Gracias a GitHub Copilot por hacer posible la automatización con IA
- Gracias a todos los desarrolladores que contribuyen

## 👨‍💻 Contribuir

¿Quieres mejorar este proyecto? Adelante:

1. **Fork** el repositorio
2. **Crea una rama**: `git checkout -b feature/tu-feature`
3. **Haz commits**: `git commit -am 'Add: descripción del cambio'`
4. **Push**: `git push origin feature/tu-feature`
5. **Abre un Pull Request**

Todos los aportes son bienvenidos, desde mejoras en la documentación hasta nuevas características.

---

## 🌟 Si te ha gustado, ¡no olvides...

- ⭐ **Dale una estrella** al repositorio
- 🍴 **Haz un fork** si quieres experimentar
- 💬 **Abre un issue** si encuentras bugs o tienes sugerencias
- 📢 **Comparte** este proyecto con tu equipo

**Sígueme en mis redes sociales para más contenido sobre Jenkins, GitHub Copilot y DevOps:**

[![YouTube Channel Subscribers](https://img.shields.io/youtube/channel/subscribers/UCHpN7VxVA7trIjc_2S2CvEQ?style=social)](https://www.youtube.com/@returngis?sub_confirmation=1)
[![LinkedIn Follow](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/giselatorresbuitrago/)
[![X Follow](https://img.shields.io/badge/X-000000?style=for-the-badge&logo=x&logoColor=white)](https://twitter.com/0GiS0)
[![TikTok](https://img.shields.io/badge/TikTok-000000?style=for-the-badge&logo=tiktok&logoColor=white)](https://www.tiktok.com/@returngis)
[![Blog](https://img.shields.io/badge/Blog-FF5722?style=for-the-badge&logo=blogger&logoColor=white)](https://www.returngis.net)

---

> Made with ❤️ by [Gisela Torres](https://www.returngis.net) | 🚀 Jenkins + GitHub Copilot integration

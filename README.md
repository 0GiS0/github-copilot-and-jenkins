# 🤖 GitHub Copilot CLI + Jenkins Integration Demo

[![CI](https://github.com/0GiS0/github-copilot-and-jenkins/actions/workflows/ci.yml/badge.svg)](https://github.com/0GiS0/github-copilot-and-jenkins/actions/workflows/ci.yml)
[![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white)](https://www.jenkins.io/)
[![GitHub Copilot](https://img.shields.io/badge/GitHub%20Copilot-000000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/features/copilot)
[![DevContainer](https://img.shields.io/badge/DevContainer-007ACC?style=for-the-badge&logo=visual-studio-code&logoColor=white)](https://containers.dev/)

Repositorio de ejemplo que demuestra cómo integrar **GitHub Copilot CLI** dentro de **Jenkins** usando Dev Containers para una configuración sencilla y reproducible.

## 🎯 ¿Qué incluye este proyecto?

- **Dev Container** preconfigurado con Jenkins y todas las dependencias
- **Jenkins Configuration as Code (JCasC)** para setup automático
- **Plugin [MCP Server](https://plugins.jenkins.io/mcp-server/)** preinstalado para conectar GitHub Copilot Chat con Jenkins vía Model Context Protocol
- **3 pipelines de demostración** usando Copilot CLI:
  - 📝 Code Review automático
  - 📚 Generación de documentación
  - 🔍 Análisis de código y sugerencias
- **Proyecto Node.js/TypeScript** de ejemplo

## 📋 Requisitos Previos

- [Docker Desktop](https://www.docker.com/products/docker-desktop) instalado
- [Visual Studio Code](https://code.visualstudio.com/) con la extensión [Dev Containers](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)
- Token de GitHub con acceso a Copilot (`GH_TOKEN`)

## 🚀 Inicio Rápido

### 1. Configurar el Token de GitHub

Necesitas un token de GitHub con permisos para usar Copilot CLI y para que Jenkins pueda descargar el contenido del repositorio privado de prueba:

1. Ve a [GitHub Settings > Developer Settings > Personal Access Tokens](https://github.com/settings/tokens)
2. Crea un nuevo token con estos permisos:
    - Fine-grained PAT: acceso al repositorio de prueba, permiso `Contents: Read` y permiso `Copilot Requests`
    - Classic PAT: scope `repo` y acceso a GitHub Copilot CLI en tu cuenta u organización
3. Configura la variable de entorno:

```bash
# En tu terminal (macOS/Linux)
export GH_TOKEN="tu_token_aqui"

# En Windows PowerShell
$env:GH_TOKEN="tu_token_aqui"
```

El Dev Container sincroniza automáticamente esta variable con la credencial `gh-token` de Jenkins al arrancar. Si `GH_TOKEN` no está definido pero `gh auth token` está disponible dentro del contenedor, el script usará ese token de GitHub CLI. Si cambias el token con el contenedor ya abierto, puedes relanzar la sincronización desde la terminal integrada:

```bash
scripts/sync-jenkins-gh-token.sh
```

### 2. Abrir el Dev Container

1. Clona este repositorio:
   ```bash
   git clone https://github.com/tu-usuario/github-copilot-jenkins-demo.git
   cd github-copilot-jenkins-demo
   ```

2. Abre VS Code en el directorio:
   ```bash
   code .
   ```

3. Cuando VS Code detecte el Dev Container, haz clic en **"Reopen in Container"**

4. Espera a que Docker construya y levante los contenedores (~5 minutos la primera vez)

### 3. Acceder a Jenkins

Una vez levantado el Dev Container:

- **URL**: http://localhost:8081
- **Usuario**: `admin`
- **Contraseña**: `admin`

### 4. Conectar GitHub Copilot Chat al MCP Server de Jenkins

El Dev Container instala automáticamente el plugin [MCP Server](https://plugins.jenkins.io/mcp-server/), que expone Jenkins como un servidor [Model Context Protocol](https://modelcontextprotocol.io/). Esto permite a GitHub Copilot Chat consultar y operar Jenkins (jobs, builds, logs, etc.) directamente desde el editor con lenguaje natural.

La configuración ya está incluida en [.vscode/mcp.json](.vscode/mcp.json) y usa el endpoint **stateless** (HTTP plano, sin sesiones) que funciona de forma fiable dentro del Dev Container:

```
http://jenkins:8080/mcp-server/stateless
```

> Por qué stateless en lugar de SSE: el endpoint SSE devuelve, durante el handshake, una URL de mensajes basada en el campo *Jenkins URL* de la configuración (`http://localhost:8081/...`), que no se resuelve desde dentro del Dev Container. El endpoint stateless usa una sola URL y evita ese problema.

**Pasos para activarlo:**

1. **Genera un API token de Jenkins** (la contraseña también funciona, pero el token es lo recomendado):
   - Entra en http://localhost:8081 con `admin` / `admin`
   - Pulsa tu nombre de usuario (arriba a la derecha) → **Security** → **Add new token** → ponle un nombre (ej. `mcp-copilot`) → **Generate** → copia el valor.

2. **Codifica las credenciales en Base64**:

   ```bash
   echo -n "admin:TU_API_TOKEN" | base64
   ```

3. En VS Code: paleta de comandos (`Cmd/Ctrl + Shift + P`) → **MCP: List Servers** → `jenkins` → **Start Server**. Cuando se te pida la credencial, pega el valor base64 anterior.

4. Abre Copilot Chat en modo **Agent** y prueba prompts como:
   - "Lista los jobs disponibles en Jenkins"
   - "Dame el estado del último build de `main-pipeline`"
   - "Muéstrame las últimas 50 líneas del log del último build de `copilot-demos/code-analysis`"
   - "Lanza el job `copilot-demos/code-review`"

> ℹ️ El plugin requiere Jenkins 2.533+. La imagen base `jenkins/jenkins:lts` ya cumple este requisito.

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

## 🔧 Pipelines Disponibles

### 📝 Code Review (`copilot-demos/code-review`)

Ejecuta una revisión automática del código usando Copilot CLI:

- Analiza cada archivo TypeScript
- Genera sugerencias de mejoras
- Crea un reporte en Markdown

### 📚 Documentation Generator (`copilot-demos/docs-generator`)

Genera documentación automáticamente:

- README con instrucciones
- Documentación de API
- Explicación de funciones

### 🔍 Code Analysis (`copilot-demos/code-analysis`)

Analiza la calidad del código:

- Métricas de complejidad
- Sugerencias de seguridad
- Recomendaciones de mejoras
- Reporte HTML publicado en Jenkins como **Copilot Code Analysis Report**
- Artefactos descargables: `analysis/report.html` y `analysis/report.md`

### 🚀 Main Pipeline

Pipeline orquestador que ejecuta todos los demos:

```
Jenkins > main-pipeline > Build with Parameters
```

Opciones:
- `DEMO_TYPE`: ALL, CODE_REVIEW, DOCS_GENERATOR, CODE_ANALYSIS
- `RUN_TESTS`: Ejecutar tests antes de los demos
- `INSTALL_DEPS`: Instalar dependencias npm

## 💡 Uso de Copilot CLI en Jenkins

Este proyecto usa el binario nuevo de GitHub Copilot CLI, `copilot`, no la extensión antigua `gh-copilot`.

### Comando básico con agentes

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

### Ejecución no interactiva en Jenkins

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

## 🔒 Seguridad

⚠️ **Importante**: Nunca subas tu `GH_TOKEN` al repositorio.

Este proyecto usa:
- **Jenkins Credentials** para almacenar el token de forma segura
- **Variable de entorno** pasada desde el host al Dev Container
- **Credential Binding** en los pipelines

Jenkins crea dos credenciales a partir de `GH_TOKEN`:

- `gh-token`: token secreto usado por los pipelines para ejecutar `copilot`
- `github-token`: usuario/password usado por Git para clonar este repositorio privado desde GitHub

## 🐛 Troubleshooting

### El Dev Container no levanta

1. Verifica que Docker Desktop esté corriendo
2. Comprueba que tienes suficiente memoria asignada a Docker (mínimo 4GB)
3. Intenta reconstruir: `Ctrl+Shift+P` > "Dev Containers: Rebuild Container"

### Jenkins no reconoce el token

1. Verifica que `GH_TOKEN` esté definido antes de abrir VS Code
2. Ejecuta `scripts/sync-jenkins-gh-token.sh` desde la terminal integrada
3. Reinicia el Dev Container si `GH_TOKEN` se definió después de abrir VS Code
4. Comprueba las credenciales en Jenkins: Manage Jenkins > Credentials

Si el repositorio es privado, el token debe ser válido para GitHub API y tener acceso de lectura al repo. Para un token classic usa el scope `repo`; para un fine-grained token concede acceso al repositorio y permiso `Contents: Read`.

### Copilot CLI no responde

1. Verifica que tu token tenga permiso `Copilot Requests`
2. Comprueba tu suscripción a GitHub Copilot
3. Ejecuta `copilot --version` para verificar que el binario está instalado

## 📚 Recursos

- [GitHub Copilot CLI Documentation](https://docs.github.com/en/copilot/github-copilot-in-the-cli)
- [Jenkins Configuration as Code](https://www.jenkins.io/projects/jcasc/)
- [Dev Containers Specification](https://containers.dev/)

## 📄 Licencia

MIT License - Siéntete libre de usar este proyecto como base para tus propias integraciones.

---

Made with ❤️ for the DevOps community

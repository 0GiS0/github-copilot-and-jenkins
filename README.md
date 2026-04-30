# 🤖 Formas de integrar GitHub Copilot en Jenkins

<div align="center">

[![YouTube Channel Subscribers](https://img.shields.io/youtube/channel/subscribers/UC140iBrEZbOtvxWsJ-Tb0lQ?style=for-the-badge&logo=youtube&logoColor=white&color=red)](https://www.youtube.com/c/GiselaTorres?sub_confirmation=1)
[![GitHub followers](https://img.shields.io/github/followers/0GiS0?style=for-the-badge&logo=github&logoColor=white)](https://github.com/0GiS0)
[![LinkedIn Follow](https://img.shields.io/badge/LinkedIn-Sígueme-blue?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/giselatorresbuitrago/)
[![X Follow](https://img.shields.io/badge/X-Sígueme-black?style=for-the-badge&logo=x&logoColor=white)](https://twitter.com/0GiS0)

</div>

---

¡Hola developer 👋🏻! En este repositorio encontrarás un entorno completo para integrar GitHub Copilot en Jenkins con un plugin Java, pipelines de ejemplo y un Dev Container listo para usar. También incluye una app TypeScript para pruebas y demos de automatización con IA.

<a href="https://youtu.be/TU_CODIGO_AQUI">
  <img src="https://img.youtube.com/vi/TU_CODIGO_AQUI/maxresdefault.jpg" alt="Formas de integrar GitHub Copilot en Jenkins" width="100%" />
</a>

> ⚠️ Nota: El vídeo aún no está publicado. Cuando tengas el código final, reemplaza `TU_CODIGO_AQUI` en el enlace y en la miniatura.

---

## 📑 Tabla de Contenidos

- [🇪🇸 Versión en Español](#es)
- [✨ Características (ES)](#caracteristicas-es)
- [🛠️ Tecnologías Utilizadas (ES)](#tecnologias-es)
- [📋 Requisitos Previos (ES)](#requisitos-es)
- [🚀 Instalación (ES)](#instalacion-es)
- [💻 Uso (ES)](#uso-es)
- [📁 Estructura del Proyecto (ES)](#estructura-es)
- [🤝 Contribuir (ES)](#contribuir-es)
- [🇬🇧 English Version](#en)
- [✨ Features (EN)](#features-en)
- [🛠️ Technologies Used (EN)](#technologies-en)
- [📋 Prerequisites (EN)](#prerequisites-en)
- [🚀 Installation (EN)](#installation-en)
- [💻 Usage (EN)](#usage-en)
- [📁 Project Structure (EN)](#structure-en)
- [🤝 Contributing (EN)](#contributing-en)
- [📚 Documentation](#-documentation)
- [🌐 Sígueme en Mis Redes Sociales](#-sígueme-en-mis-redes-sociales)

---

<a id="es"></a>
## 🇪🇸 Versión en Español

<a id="caracteristicas-es"></a>
### ✨ Características (ES)

- Plugin **Copilot Chat** para Jenkins (Java) con respuestas en streaming.
- Integración con **GitHub Copilot SDK Java** y soporte de herramientas MCP.
- Pipelines de demostración para **code review**, **generación de documentación** y **análisis de código**.
- Entorno reproducible con **Dev Container + Docker Compose**.
- Proyecto de ejemplo en **Node.js + TypeScript + Jest** para experimentar con prompts y flujos CI.
- Sincronización automática del `GH_TOKEN` con credenciales de Jenkins mediante script.

<a id="tecnologias-es"></a>
### 🛠️ Tecnologías Utilizadas (ES)

- **Node.js** (LTS, configurado en Dev Container)
- **TypeScript 5.3.3**
- **Jest 29.7.0**
- **ESLint 8.56.0**
- **Java 17**
- **Maven** (plugin Jenkins HPI)
- **Jenkins 2.479.3**
- **Docker / Docker Compose**
- **GitHub Actions** (`.github/workflows/ci.yml`)

<a id="requisitos-es"></a>
### 📋 Requisitos Previos (ES)

- **Git**
- **Docker Desktop** (recomendado para levantar todo el entorno)
- **VS Code** + extensión **Dev Containers** (ruta recomendada)
- **GH_TOKEN** con permisos de repositorio y **Copilot Requests**

Opcional para ejecución local sin Dev Container:

- **Node.js 20+**
- **npm**
- **Java 17** y **Maven** (si vas a compilar el plugin)

<a id="instalacion-es"></a>
### 🚀 Instalación (ES)

### Paso 1: Clonar el repositorio
```bash
git clone https://github.com/0GiS0/github-copilot-and-jenkins.git
cd github-copilot-and-jenkins
```

### Paso 2: Configurar token de GitHub
```bash
# macOS/Linux
export GH_TOKEN="tu_token_aqui"

# Windows PowerShell
$env:GH_TOKEN="tu_token_aqui"
```

### Paso 3 (recomendado): Abrir en Dev Container
```bash
code .
```

Luego selecciona **Reopen in Container** en VS Code.

### Paso 4 (alternativa Docker Compose)
```bash
cd .devcontainer
docker compose up -d --build
```

Jenkins quedará disponible en `http://localhost:8081` (usuario `admin`, contraseña `admin`).

### Paso 5: Dependencias del proyecto Node.js
```bash
npm install
```

### Paso 6: Sincronizar token con Jenkins (si aplica)
```bash
./scripts/sync-jenkins-gh-token.sh
```

<a id="uso-es"></a>
### 💻 Uso (ES)

Comandos principales del proyecto:

```bash
npm run dev     # Ejecuta la demo TypeScript
npm run build   # Compila TypeScript a dist/
npm start       # Ejecuta dist/index.js
npm test        # Ejecuta tests con Jest
npm run lint    # Lint sobre src/**/*.ts
npm run clean   # Limpia dist/
```

Pipelines de demo incluidos:

- `pipelines/code-review.jenkinsfile`
- `pipelines/docs-generator.jenkinsfile`
- `pipelines/code-analysis.jenkinsfile`
- `Jenkinsfile` (orquestación principal por parámetro `DEMO_TYPE`)

<a id="estructura-es"></a>
### 📁 Estructura del Proyecto (ES)

```text
.
├── .devcontainer/
│   ├── devcontainer.json
│   ├── docker-compose.yml
│   ├── Dockerfile.devcontainer
│   ├── Dockerfile.jenkins
│   └── jenkins-config/
├── .github/workflows/
│   └── ci.yml
├── jenkins-copilot-chat-plugin/
│   ├── pom.xml
│   └── src/
├── pipelines/
│   ├── code-analysis.jenkinsfile
│   ├── code-review.jenkinsfile
│   └── docs-generator.jenkinsfile
├── scripts/
│   ├── install-copilot-cli.sh
│   └── sync-jenkins-gh-token.sh
├── src/
│   ├── index.ts
│   ├── utils.ts
│   └── utils.test.ts
├── Jenkinsfile
├── package.json
└── tsconfig.json
```

<a id="contribuir-es"></a>
### 🤝 Contribuir (ES)

¡Las contribuciones son bienvenidas! Si quieres mejorar este proyecto:

1. Haz un fork del repositorio
2. Crea una rama para tu feature (`git checkout -b feature/mi-mejora`)
3. Haz commit de tus cambios (`git commit -m 'Añadir mi mejora'`)
4. Haz push a la rama (`git push origin feature/mi-mejora`)
5. Abre un Pull Request

---

<a id="en"></a>
## 🇬🇧 English Version

<a id="features-en"></a>
### ✨ Features (EN)

- **Copilot Chat** Jenkins plugin (Java) with streaming responses.
- Integration with **GitHub Copilot SDK Java** and MCP tooling.
- Demo pipelines for **code review**, **docs generation**, and **code analysis**.
- Reproducible environment with **Dev Container + Docker Compose**.
- **Node.js + TypeScript + Jest** sample app for CI/automation experiments.
- Automatic `GH_TOKEN` sync to Jenkins credentials via script.

<a id="technologies-en"></a>
### 🛠️ Technologies Used (EN)

- **Node.js** (LTS in Dev Container)
- **TypeScript 5.3.3**
- **Jest 29.7.0**
- **ESLint 8.56.0**
- **Java 17**
- **Maven**
- **Jenkins 2.479.3**
- **Docker / Docker Compose**
- **GitHub Actions**

<a id="prerequisites-en"></a>
### 📋 Prerequisites (EN)

- **Git**
- **Docker Desktop** (recommended full setup)
- **VS Code** + **Dev Containers** extension
- **GH_TOKEN** with repository access and **Copilot Requests**

Optional for local execution without Dev Container:

- **Node.js 20+**
- **npm**
- **Java 17** and **Maven** (for plugin build)

<a id="installation-en"></a>
### 🚀 Installation (EN)

### Step 1: Clone the repository
```bash
git clone https://github.com/0GiS0/github-copilot-and-jenkins.git
cd github-copilot-and-jenkins
```

### Step 2: Configure GitHub token
```bash
# macOS/Linux
export GH_TOKEN="your_token_here"

# Windows PowerShell
$env:GH_TOKEN="your_token_here"
```

### Step 3 (recommended): Open in Dev Container
```bash
code .
```

Then choose **Reopen in Container** in VS Code.

### Step 4 (Docker Compose alternative)
```bash
cd .devcontainer
docker compose up -d --build
```

Jenkins will be available at `http://localhost:8081` (`admin` / `admin`).

### Step 5: Install Node.js dependencies
```bash
npm install
```

### Step 6: Sync token to Jenkins (if needed)
```bash
./scripts/sync-jenkins-gh-token.sh
```

<a id="usage-en"></a>
### 💻 Usage (EN)

Core commands:

```bash
npm run dev
npm run build
npm start
npm test
npm run lint
npm run clean
```

Included demo pipelines:

- `pipelines/code-review.jenkinsfile`
- `pipelines/docs-generator.jenkinsfile`
- `pipelines/code-analysis.jenkinsfile`
- `Jenkinsfile` (main orchestrator with `DEMO_TYPE`)

<a id="structure-en"></a>
### 📁 Project Structure (EN)

```text
.
├── .devcontainer/
├── .github/workflows/
├── jenkins-copilot-chat-plugin/
├── pipelines/
├── scripts/
├── src/
├── Jenkinsfile
├── package.json
└── tsconfig.json
```

<a id="contributing-en"></a>
### 🤝 Contributing (EN)

Contributions are welcome! If you want to improve this project:

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/my-improvement`)
3. Commit your changes (`git commit -m "Add my improvement"`)
4. Push to your branch (`git push origin feature/my-improvement`)
5. Open a Pull Request

---

## 📚 Documentation

In-depth guides are split into separate files to keep this README concise:

| Guide | Description |
|-------|-------------|
| [🔌 How Jenkins Plugins Work](docs/how-jenkins-plugins-work.md) | Extension points, Stapler MVC, Jelly templates, `GlobalConfiguration`, `UserProperty`, `PageDecorator` — the fundamentals you need to understand any Jenkins plugin. |
| [🤖 Copilot Chat Plugin — Internals](docs/copilot-chat-plugin-internals.md) | How all those building blocks were applied to build this plugin: GitHub OAuth Device Flow, SDK session lifecycle, MCP server wiring, Server-Sent Events streaming, and security model. |

---

## 🌐 Sígueme en Mis Redes Sociales

Si te ha gustado este proyecto y quieres ver más contenido como este, no olvides suscribirte a mi canal de YouTube y seguirme en mis redes sociales:

<div align="center">

[![YouTube Channel Subscribers](https://img.shields.io/youtube/channel/subscribers/UC140iBrEZbOtvxWsJ-Tb0lQ?style=for-the-badge&logo=youtube&logoColor=white&color=red)](https://www.youtube.com/c/GiselaTorres?sub_confirmation=1)
[![GitHub followers](https://img.shields.io/github/followers/0GiS0?style=for-the-badge&logo=github&logoColor=white)](https://github.com/0GiS0)
[![LinkedIn Follow](https://img.shields.io/badge/LinkedIn-Sígueme-blue?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/giselatorresbuitrago/)
[![X Follow](https://img.shields.io/badge/X-Sígueme-black?style=for-the-badge&logo=x&logoColor=white)](https://twitter.com/0GiS0)

</div>

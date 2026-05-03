# 🤖 Formas de integrar GitHub Copilot en Jenkins

<div align="center">

[![YouTube Channel Subscribers](https://img.shields.io/youtube/channel/subscribers/UC140iBrEZbOtvxWsJ-Tb0lQ?style=for-the-badge&logo=youtube&logoColor=white&color=red)](https://www.youtube.com/c/GiselaTorres?sub_confirmation=1)
[![GitHub followers](https://img.shields.io/github/followers/0GiS0?style=for-the-badge&logo=github&logoColor=white)](https://github.com/0GiS0)
[![LinkedIn Follow](https://img.shields.io/badge/LinkedIn-Sígueme-blue?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/giselatorresbuitrago/)
[![X Follow](https://img.shields.io/badge/X-Sígueme-black?style=for-the-badge&logo=x&logoColor=white)](https://twitter.com/0GiS0)

🇬🇧 [Read this in English](README.en.md)

</div>

---

¡Hola developer 👋🏻! En este repositorio encontrarás un entorno completo para integrar GitHub Copilot en Jenkins con un plugin Java, pipelines de ejemplo integradas con Github Copilot CLI y un Dev Container listo para usar. También incluye una app TypeScript para pruebas y demos de automatización con IA.

<a href="https://youtu.be/TU_CODIGO_AQUI">
  <img src="https://img.youtube.com/vi/TU_CODIGO_AQUI/maxresdefault.jpg" alt="Formas de integrar GitHub Copilot en Jenkins" width="100%" />
</a>

> ⚠️ Nota: El vídeo aún no está publicado. Cuando tengas el código final, reemplaza `TU_CODIGO_AQUI` en el enlace y en la miniatura.

---

## 📑 Tabla de Contenidos

- [✨ Características](#características)
- [🛠️ Tecnologías Utilizadas](#️-tecnologías-utilizadas)
- [📋 Requisitos Previos](#-requisitos-previos)
- [🚀 Instalación](#-instalación)
- [💻 Uso](#-uso)
- [📁 Estructura del Proyecto](#-estructura-del-proyecto)
- [🤝 Contribuir](#-contribuir)
- [📚 Documentación](#-documentación)
- [🌐 Sígueme en Mis Redes Sociales](#-sígueme-en-mis-redes-sociales)

---

## ✨ Características

- Plugin **Copilot Chat** para Jenkins (Java) con respuestas en streaming.
- Integración con **GitHub Copilot SDK Java** y soporte de herramientas MCP.
- Pipelines de demostración para **code review**, **generación de documentación** y **análisis de código**.
- Entorno reproducible con **Dev Container + Docker Compose**.
- Proyecto de ejemplo en **Node.js + TypeScript + Jest** para experimentar con prompts y flujos CI.
- Sincronización automática del `GH_TOKEN` con credenciales de Jenkins mediante script.

## 🛠️ Tecnologías Utilizadas

- **Node.js** (LTS, configurado en Dev Container)
- **TypeScript 5.3.3**
- **Jest 29.7.0**
- **ESLint 8.56.0**
- **Java 17**
- **Maven** (plugin Jenkins HPI)
- **Jenkins 2.479.3**
- **Docker / Docker Compose**
- **GitHub Actions** (`.github/workflows/ci.yml`)

## 📋 Requisitos Previos

- **Git**
- **Docker Desktop** (recomendado para levantar todo el entorno)
- **VS Code** + extensión **Dev Containers** (ruta recomendada)
- **GH_TOKEN** con permisos de repositorio y **Copilot Requests**

Opcional para ejecución local sin Dev Container:

- **Node.js 20+**
- **npm**
- **Java 17** y **Maven** (si vas a compilar el plugin)

## 🚀 Instalación

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

## 💻 Uso

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

## 📁 Estructura del Proyecto

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

## 📚 Más información

Si quieres profundizar en cómo funcionan los plugins de Jenkins y entender los conceptos clave detrás de este proyecto, te recomiendo revisar la documentación incluida en este repositorio:

| Guía | Descripción |
|-------|-------------|
| [🔌 Cómo funcionan los plugins de Jenkins](docs/how-jenkins-plugins-work.md) | Extension points, Stapler MVC, plantillas Jelly, `GlobalConfiguration`, `UserProperty`, `PageDecorator` — los fundamentos para entender cualquier plugin de Jenkins. |
| [🤖 Copilot Chat Plugin — Internals](docs/copilot-chat-plugin-internals.md) | Cómo se aplicaron esos bloques para construir este plugin: GitHub OAuth Device Flow, ciclo de vida de sesión del SDK, MCP server, streaming con Server-Sent Events y modelo de seguridad. |

---

## 🌐 Sígueme en Mis Redes Sociales

Si te ha gustado este proyecto y quieres ver más contenido como este, no olvides suscribirte a mi canal de YouTube y seguirme en mis redes sociales:

<div align="center">

[![YouTube Channel Subscribers](https://img.shields.io/youtube/channel/subscribers/UC140iBrEZbOtvxWsJ-Tb0lQ?style=for-the-badge&logo=youtube&logoColor=white&color=red)](https://www.youtube.com/c/GiselaTorres?sub_confirmation=1)
[![GitHub followers](https://img.shields.io/github/followers/0GiS0?style=for-the-badge&logo=github&logoColor=white)](https://github.com/0GiS0)
[![LinkedIn Follow](https://img.shields.io/badge/LinkedIn-Sígueme-blue?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/giselatorresbuitrago/)
[![X Follow](https://img.shields.io/badge/X-Sígueme-black?style=for-the-badge&logo=x&logoColor=white)](https://twitter.com/0GiS0)

</div>

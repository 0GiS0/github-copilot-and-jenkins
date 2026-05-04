# 🤖 Ways to integrate GitHub Copilot in Jenkins

<div align="center">

[![YouTube Channel Subscribers](https://img.shields.io/youtube/channel/subscribers/UC140iBrEZbOtvxWsJ-Tb0lQ?style=for-the-badge&logo=youtube&logoColor=white&color=red)](https://www.youtube.com/c/GiselaTorres?sub_confirmation=1)
[![GitHub followers](https://img.shields.io/github/followers/0GiS0?style=for-the-badge&logo=github&logoColor=white)](https://github.com/0GiS0)
[![LinkedIn Follow](https://img.shields.io/badge/LinkedIn-Follow%20me-blue?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/giselatorresbuitrago/)
[![X Follow](https://img.shields.io/badge/X-Follow%20me-black?style=for-the-badge&logo=x&logoColor=white)](https://twitter.com/0GiS0)

🇪🇸 [Leer en Español](README.md)

</div>

---

Hey developer 👋🏻! This repository contains a complete environment for integrating GitHub Copilot in Jenkins with a Java plugin, sample pipelines, and a ready-to-use Dev Container. It also includes a TypeScript app for testing and AI automation demos.

<a href="https://youtu.be/TU_CODIGO_AQUI">
  <img src="https://img.youtube.com/vi/TU_CODIGO_AQUI/maxresdefault.jpg" alt="Ways to integrate GitHub Copilot in Jenkins" width="100%" />
</a>

> ⚠️ Note: The video is not yet published. Once you have the final code, replace `TU_CODIGO_AQUI` in both the link and the thumbnail.

---

## 📑 Table of Contents

- [✨ Features](#-features)
- [🛠️ Technologies Used](#️-technologies-used)
- [📋 Prerequisites](#-prerequisites)
- [🚀 Installation](#-installation)
- [💻 Usage](#-usage)
- [📁 Project Structure](#-project-structure)
- [🤝 Contributing](#-contributing)
- [📚 Documentation](#-documentation)
- [🌐 Follow Me on Social Media](#-follow-me-on-social-media)

---

## ✨ Features

- **Copilot Chat** Jenkins plugin (Java) with streaming responses.
- Integration with **GitHub Copilot SDK Java** and MCP tooling.
- Demo pipelines for **code review**, **docs generation**, and **code analysis**.
- Reproducible environment with **Dev Container + Docker Compose**.
- **Node.js + TypeScript + Jest** sample app for CI/automation experiments.
- Automatic `GH_TOKEN` sync to Jenkins credentials via script.

## 🛠️ Technologies Used

- **Node.js** (LTS, configured in Dev Container)
- **TypeScript 5.3.3**
- **Jest 29.7.0**
- **ESLint 8.56.0**
- **Java 17**
- **Maven** (Jenkins HPI plugin)
- **Jenkins 2.479.3**
- **Docker / Docker Compose**
- **GitHub Actions** (`.github/workflows/ci.yml`)

## 📋 Prerequisites

- **Git**
- **Docker Desktop** (recommended for the full environment)
- **VS Code** + **Dev Containers** extension (recommended path)
- **GH_TOKEN** with repository permissions and **Copilot Requests**

Optional for local execution without Dev Container:

- **Node.js 20+**
- **npm**
- **Java 17** and **Maven** (if you are building the plugin)

## 🚀 Installation

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

Then select **Reopen in Container** in VS Code.

### Step 4 (Docker Compose alternative)
```bash
cd .devcontainer
docker compose up -d --build
```

Jenkins will be available at `http://localhost:8081` (user `admin`, password `admin`).

### Step 5: Install Node.js dependencies
```bash
npm install
```

### Step 6: Sync token with Jenkins (if needed)
```bash
./scripts/sync-jenkins-gh-token.sh
```

## 💻 Usage

Core project commands:

```bash
npm run dev     # Runs the TypeScript demo
npm run build   # Compiles TypeScript to dist/
npm start       # Runs dist/index.js
npm test        # Runs tests with Jest
npm run lint    # Lint on src/**/*.ts
npm run clean   # Cleans dist/
```

Included demo pipelines:

- `pipelines/code-review.jenkinsfile`
- `pipelines/docs-generator.jenkinsfile`
- `pipelines/code-analysis.jenkinsfile`

## 📁 Project Structure

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
├── package.json
└── tsconfig.json
```

## 🤝 Contributing

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
| [🔌 How Jenkins Plugins Work](docs/how-jenkins-plugins-work.en.md) | Extension points, Stapler MVC, Jelly templates, `GlobalConfiguration`, `UserProperty`, `PageDecorator` — the fundamentals you need to understand any Jenkins plugin. |
| [🤖 Copilot Chat Plugin — Internals](docs/copilot-chat-plugin-internals.en.md) | How all those building blocks were applied to build this plugin: GitHub OAuth Device Flow, SDK session lifecycle, MCP server wiring, Server-Sent Events streaming, and security model. |

---

## 🌐 Follow Me on Social Media

If you enjoyed this project and want to see more content like this, don't forget to subscribe to my YouTube channel and follow me on social media:

<div align="center">

[![YouTube Channel Subscribers](https://img.shields.io/youtube/channel/subscribers/UC140iBrEZbOtvxWsJ-Tb0lQ?style=for-the-badge&logo=youtube&logoColor=white&color=red)](https://www.youtube.com/c/GiselaTorres?sub_confirmation=1)
[![GitHub followers](https://img.shields.io/github/followers/0GiS0?style=for-the-badge&logo=github&logoColor=white)](https://github.com/0GiS0)
[![LinkedIn Follow](https://img.shields.io/badge/LinkedIn-Follow%20me-blue?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/giselatorresbuitrago/)
[![X Follow](https://img.shields.io/badge/X-Follow%20me-black?style=for-the-badge&logo=x&logoColor=white)](https://twitter.com/0GiS0)

</div>

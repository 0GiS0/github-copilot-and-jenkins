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

<a id="it"></a>
## 🇮🇹 Versione Italiana

<a id="caratteristiche-it"></a>
### ✨ Caratteristiche (IT)

- Demo completa di integrazione **GitHub Copilot + Jenkins**.
- Plugin Jenkins in **Java 17** (`jenkins-copilot-chat-plugin`) con SDK Copilot.
- Pipeline demo per **code review**, **documentazione** e **code analysis**.
- App esempio **Node.js + TypeScript + Jest** in `src/`.
- Script per installazione Copilot CLI e sincronizzazione del `GH_TOKEN` in Jenkins.

<a id="tecnologie-it"></a>
### 🛠️ Tecnologie Utilizzate (IT)

- **Node.js** (LTS nel Dev Container)
- **TypeScript 5.3.3**
- **Jest 29.7.0**
- **ESLint 8.56.0**
- **Java 17**
- **Maven** (plugin Jenkins HPI)
- **Jenkins 2.479.3**
- **Docker / Docker Compose**
- **GitHub Actions**

<a id="prerequisiti-it"></a>
### 📋 Prerequisiti (IT)

- **Git**
- **Docker Desktop** (consigliato per setup completo)
- **VS Code** + estensione **Dev Containers**
- **GH_TOKEN** con permessi repository e **Copilot Requests**

Opzionale per esecuzione locale senza Dev Container:

- **Node.js 20+**
- **npm**
- **Java 17** e **Maven** (per compilare il plugin)

<a id="installazione-it"></a>
### 🚀 Installazione (IT)

### Passo 1: Clonare il repository
```bash
git clone https://github.com/0GiS0/github-copilot-and-jenkins.git
cd github-copilot-and-jenkins
```

### Passo 2: Configurare il token GitHub
```bash
# macOS/Linux
export GH_TOKEN="il_tuo_token"

# Windows PowerShell
$env:GH_TOKEN="il_tuo_token"
```

### Passo 3 (consigliato): Aprire nel Dev Container
```bash
code .
```

Poi seleziona **Reopen in Container** in VS Code.

### Passo 4 (alternativa Docker Compose)
```bash
cd .devcontainer
docker compose up -d --build
```

Jenkins sarà disponibile su `http://localhost:8081` (`admin` / `admin`).

### Passo 5: Installare le dipendenze Node.js
```bash
npm install
```

### Passo 6: Sincronizzare il token con Jenkins (se necessario)
```bash
./scripts/sync-jenkins-gh-token.sh
```

<a id="utilizzo-it"></a>
### 💻 Utilizzo (IT)

Comandi principali:

```bash
npm run dev
npm run build
npm start
npm test
npm run lint
npm run clean
```

Pipeline demo incluse:

- `pipelines/code-review.jenkinsfile`
- `pipelines/docs-generator.jenkinsfile`
- `pipelines/code-analysis.jenkinsfile`
- `Jenkinsfile` (orchestrazione principale con `DEMO_TYPE`)

<a id="struttura-it"></a>
### 📁 Struttura del Progetto (IT)

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

<a id="contribuire-it"></a>
### 🤝 Contribuire (IT)

I contributi sono benvenuti! Per migliorare questo progetto:

1. Fai un fork del repository
2. Crea un branch feature (`git checkout -b feature/mio-miglioramento`)
3. Commit delle modifiche (`git commit -m "Aggiunge miglioramento"`)
4. Push del branch (`git push origin feature/mio-miglioramento`)
5. Apri una Pull Request

---

<a id="fr"></a>
## 🇫🇷 Version Française

<a id="fonctionnalites-fr"></a>
### ✨ Fonctionnalités (FR)

- Plugin **Copilot Chat** pour Jenkins (Java) avec réponses en streaming.
- Intégration avec le **SDK Java GitHub Copilot** et support des outils MCP.
- Pipelines de démonstration pour la **revue de code**, la **génération de documentation** et l'**analyse de code**.
- Environnement reproductible avec **Dev Container + Docker Compose**.
- Application exemple **Node.js + TypeScript + Jest** pour expérimenter des prompts et des flux CI.
- Synchronisation automatique du `GH_TOKEN` avec les credentials Jenkins via script.

<a id="technologies-fr"></a>
### 🛠️ Technologies Utilisées (FR)

- **Node.js** (LTS, configuré dans le Dev Container)
- **TypeScript 5.3.3**
- **Jest 29.7.0**
- **ESLint 8.56.0**
- **Java 17**
- **Maven** (plugin Jenkins HPI)
- **Jenkins 2.479.3**
- **Docker / Docker Compose**
- **GitHub Actions** (`.github/workflows/ci.yml`)

<a id="prerequis-fr"></a>
### 📋 Prérequis (FR)

- **Git**
- **Docker Desktop** (recommandé pour démarrer l'environnement complet)
- **VS Code** + extension **Dev Containers** (méthode recommandée)
- **GH_TOKEN** avec accès dépôt et permissions **Copilot Requests**

Optionnel (exécution locale sans Dev Container) :

- **Node.js 20+** et **npm**
- **Java 17** et **Maven** (pour compiler le plugin)

<a id="installation-fr"></a>
### 🚀 Installation (FR)

### Étape 1 : Cloner le dépôt
```bash
git clone https://github.com/0GiS0/github-copilot-and-jenkins.git
cd github-copilot-and-jenkins
```

### Étape 2 : Configurer le token GitHub
```bash
# macOS/Linux
export GH_TOKEN="votre_token_ici"

# Windows PowerShell
$env:GH_TOKEN="votre_token_ici"
```

### Étape 3 (recommandé) : Ouvrir dans le Dev Container
```bash
code .
```
Puis sélectionnez **Reopen in Container** dans VS Code.

### Étape 4 (alternative Docker Compose)
```bash
cd .devcontainer
docker compose up -d --build
```
Jenkins sera disponible sur `http://localhost:8081` (utilisateur `admin`, mot de passe `admin`).

### Étape 5 : Installer les dépendances Node.js
```bash
npm install
```

### Étape 6 : Synchroniser le token avec Jenkins (si nécessaire)
```bash
./scripts/sync-jenkins-gh-token.sh
```

<a id="utilisation-fr"></a>
### 💻 Utilisation (FR)

Commandes principales du projet :

```bash
npm run dev     # Lance la démo TypeScript
npm run build   # Compile TypeScript vers dist/
npm start       # Exécute dist/index.js
npm test        # Lance les tests avec Jest
npm run lint    # Analyse statique sur src/**/*.ts
npm run clean   # Supprime dist/
```

Pipelines de démonstration inclus :

- `pipelines/code-review.jenkinsfile`
- `pipelines/docs-generator.jenkinsfile`
- `pipelines/code-analysis.jenkinsfile`
- `Jenkinsfile` (orchestrateur principal avec `DEMO_TYPE`)

<a id="structure-fr"></a>
### 📁 Structure du Projet (FR)

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

<a id="contribuer-fr"></a>
### 🤝 Contribuer (FR)

Les contributions sont les bienvenues ! Pour améliorer ce projet :

1. Faites un fork du dépôt
2. Créez une branche pour votre fonctionnalité (`git checkout -b feature/mon-amelioration`)
3. Commitez vos changements (`git commit -m 'Ajouter mon amélioration'`)
4. Poussez la branche (`git push origin feature/mon-amelioration`)
5. Ouvrez une Pull Request

---

## 🌐 Sígueme en Mis Redes Sociales

Si te ha gustado este proyecto y quieres ver más contenido como este, no olvides suscribirte a mi canal de YouTube y seguirme en mis redes sociales:

<div align="center">

[![YouTube Channel Subscribers](https://img.shields.io/youtube/channel/subscribers/UC140iBrEZbOtvxWsJ-Tb0lQ?style=for-the-badge&logo=youtube&logoColor=white&color=red)](https://www.youtube.com/c/GiselaTorres?sub_confirmation=1)
[![GitHub followers](https://img.shields.io/github/followers/0GiS0?style=for-the-badge&logo=github&logoColor=white)](https://github.com/0GiS0)
[![LinkedIn Follow](https://img.shields.io/badge/LinkedIn-Sígueme-blue?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/giselatorresbuitrago/)
[![X Follow](https://img.shields.io/badge/X-Sígueme-black?style=for-the-badge&logo=x&logoColor=white)](https://twitter.com/0GiS0)

</div>

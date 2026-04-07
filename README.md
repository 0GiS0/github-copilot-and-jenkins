# 🤖 GitHub Copilot CLI + Jenkins Integration Demo

[![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white)](https://www.jenkins.io/)
[![GitHub Copilot](https://img.shields.io/badge/GitHub%20Copilot-000000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/features/copilot)
[![DevContainer](https://img.shields.io/badge/DevContainer-007ACC?style=for-the-badge&logo=visual-studio-code&logoColor=white)](https://containers.dev/)

Repositorio de ejemplo que demuestra cómo integrar **GitHub Copilot CLI** dentro de **Jenkins** usando Dev Containers para una configuración sencilla y reproducible.

## 🎯 ¿Qué incluye este proyecto?

- **Dev Container** preconfigurado con Jenkins y todas las dependencias
- **Jenkins Configuration as Code (JCasC)** para setup automático
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

Necesitas un token de GitHub con permisos para usar Copilot CLI:

1. Ve a [GitHub Settings > Developer Settings > Personal Access Tokens](https://github.com/settings/tokens)
2. Crea un nuevo token (classic) con el scope `copilot`
3. Configura la variable de entorno:

```bash
# En tu terminal (macOS/Linux)
export GH_TOKEN="tu_token_aqui"

# En Windows PowerShell
$env:GH_TOKEN="tu_token_aqui"
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

- **URL**: http://localhost:8080
- **Usuario**: `admin`
- **Contraseña**: `admin`

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

### Comando básico: `gh copilot explain`

```groovy
stage('Copilot Analysis') {
    steps {
        withCredentials([string(credentialsId: 'gh-token', variable: 'GH_TOKEN')]) {
            sh '''
                gh copilot explain "What does this code do: $(cat src/utils.ts)"
            '''
        }
    }
}
```

### Comando: `gh copilot suggest`

```groovy
stage('Get Command Suggestion') {
    steps {
        withCredentials([string(credentialsId: 'gh-token', variable: 'GH_TOKEN')]) {
            sh '''
                gh copilot suggest -t shell "find all TypeScript files with more than 100 lines"
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

## 🐛 Troubleshooting

### El Dev Container no levanta

1. Verifica que Docker Desktop esté corriendo
2. Comprueba que tienes suficiente memoria asignada a Docker (mínimo 4GB)
3. Intenta reconstruir: `Ctrl+Shift+P` > "Dev Containers: Rebuild Container"

### Jenkins no reconoce el token

1. Verifica que `GH_TOKEN` esté definido antes de abrir VS Code
2. Reinicia el Dev Container después de definir la variable
3. Comprueba las credenciales en Jenkins: Manage Jenkins > Credentials

### Copilot CLI no responde

1. Verifica que tu token tenga el scope `copilot`
2. Comprueba tu suscripción a GitHub Copilot
3. Ejecuta `gh auth status` para verificar la autenticación

## 📚 Recursos

- [GitHub Copilot CLI Documentation](https://docs.github.com/en/copilot/github-copilot-in-the-cli)
- [Jenkins Configuration as Code](https://www.jenkins.io/projects/jcasc/)
- [Dev Containers Specification](https://containers.dev/)

## 📄 Licencia

MIT License - Siéntete libre de usar este proyecto como base para tus propias integraciones.

---

Made with ❤️ for the DevOps community

# 🔌 Cómo funcionan los plugins de Jenkins

🇬🇧 [Read in English](how-jenkins-plugins-work.en.md)

Este documento explica los conceptos clave del desarrollo de plugins para Jenkins.
Entender estos bloques fundamentales es esencial para comprender cómo está estructurado el plugin **GitHub Copilot Chat**.

---

## 📦 ¿Qué es un plugin de Jenkins?

Un plugin de Jenkins es un archivo `.hpi` (Hudson Plugin Interface) o `.jpi` — esencialmente un JAR con metadatos extra empaquetado en una estructura similar a un WAR. Cuando Jenkins arranca, descomprime todos los plugins que encuentra en `$JENKINS_HOME/plugins/` y los carga en su classloader.

El proyecto se construye con **Maven** usando el POM padre `jenkins-plugin`, que proporciona:
- Un goal Maven `hpi:hpi` preconfigurado que empaqueta todo.
- Gestión de dependencias para la API del núcleo de Jenkins.
- Un directorio `WEB-INF/lib/` para los JARs del plugin.
- Un `META-INF/MANIFEST.MF` con los metadatos del plugin (nombre, versión, dependencias).

```
plugin.hpi
├── META-INF/
│   └── MANIFEST.MF          ← metadatos del plugin
├── WEB-INF/
│   └── lib/
│       └── my-plugin.jar    ← clases Java compiladas + vistas Jelly
└── images/, css/, js/       ← recursos estáticos (webapp/)
```

---

## 🔧 Extension Points — `@Extension`

El núcleo del sistema de plugins de Jenkins es su modelo de **extension points**.
Jenkins define un conjunto de clases abstractas e interfaces llamadas *extension points*.
Cualquier plugin puede contribuir una implementación anotándola con `@Extension`:

```java
@Extension
public class MyRootAction implements RootAction { ... }
```

Jenkins usa `ServiceLoader` + escaneo de anotaciones al arrancar para descubrir todas
las clases anotadas con `@Extension` y registrarlas automáticamente. No se necesita ningún cableado manual.

Extension points clave usados en este repositorio:

| Extension Point | Qué hace |
|-----------------|----------|
| `RootAction` | Añade una URL bajo la raíz de Jenkins (p. ej. `/copilot-chat/`) y un enlace opcional en la barra lateral |
| `PageDecorator` | Inyecta HTML en todas las páginas (cabecera/pie) — se usa para añadir el widget de chat |
| `GlobalConfiguration` | Añade una sección en *Administrar Jenkins → Sistema* con ajustes persistentes |
| `UserProperty` | Adjunta datos a cada cuenta de usuario de Jenkins |

---

## 🌐 Stapler — El framework MVC

Jenkins usa **Stapler** como su framework web (en lugar de Spring MVC o Jakarta EE). Stapler es un framework de enlace de URLs que mapea peticiones HTTP a objetos Java por convención:

```
GET /copilot-chat/startLogin
     ↓
CopilotChatRootAction.doStartLogin()
```

Reglas:
- Un método público llamado `doXxx()` maneja `GET /xxx`.
- Un método que acepta `StaplerRequest2` / `StaplerResponse2` maneja `POST`.
- Los métodos pueden devolver objetos `HttpResponse` o escribir directamente en la respuesta.
- `@QueryParameter` inyecta parámetros de query URL como argumentos del método.

Esto significa que añades un nuevo endpoint simplemente añadiendo un método — sin archivos de configuración de rutas.

---

## 🎨 Plantillas Jelly

Las vistas en los plugins de Jenkins se escriben en **Apache Jelly** — un lenguaje de plantillas basado en XML.
Los archivos Jelly viven en `src/main/resources/` y Stapler los resuelve según la jerarquía de clases:

```
src/main/resources/
└── io/jenkins/plugins/copilotchat/
    ├── CopilotChatConfiguration/
    │   └── config.jelly          ← renderizado en Administrar Jenkins → Sistema
    ├── CopilotChatPageDecorator/
    │   └── footer.jelly          ← inyectado en el pie de todas las páginas
    └── CopilotChatRootAction/
        └── index.jelly           ← renderizado en /copilot-chat/
```

Una plantilla Jelly para `MiClase` debe estar en `io/mi/paquete/MiClase/vista.jelly`.
Stapler encuentra la plantilla correcta automáticamente.

---

## ⚙️ GlobalConfiguration — Ajustes Persistentes

`GlobalConfiguration` es un extension point de Jenkins que:
1. Registra un panel de configuración en *Administrar Jenkins → Sistema*.
2. Persiste los campos en un archivo XML en `$JENKINS_HOME/` mediante serialización XStream.
3. Expone un método estático `get()` para que otras clases lean los ajustes.

```java
@Extension
public class MyConfig extends GlobalConfiguration {
    private String apiUrl;

    public MyConfig() { load(); }           // lee el XML al arrancar

    @DataBoundSetter
    public void setApiUrl(String v) {
        this.apiUrl = v;
        save();                             // escribe el XML inmediatamente
    }
}
```

`@DataBoundSetter` indica a Stapler que vincule el campo de formulario con el mismo nombre a este setter cuando el usuario guarda el formulario de configuración.

---

## 👤 UserProperty — Datos por Usuario

`UserProperty` adjunta datos arbitrarios a las cuentas de usuario de Jenkins. Los datos de cada usuario se persisten en `$JENKINS_HOME/users/<id>/config.xml`.

```java
public class MyProperty extends UserProperty {
    private final String value;
    // ...
}

// Guardar:
user.addProperty(new MyProperty("hello"));
user.save();

// Leer:
MyProperty p = user.getProperty(MyProperty.class);
```

Jenkins cifra los campos sensibles usando `hudson.util.Secret`:
```java
private Secret token;                           // almacenado cifrado en disco
String plain = Secret.toString(token);          // descifrado en tiempo de ejecución
Secret wrapped = Secret.fromString(plainText);  // envolver antes de guardar
```

---

## 🖼️ PageDecorator — Inyectando UI en Todas las Páginas

`PageDecorator` permite a un plugin añadir HTML en la cabecera o el pie de **todas** las páginas de Jenkins.
La decoración ocurre a través de una plantilla Jelly en `NombreClase/footer.jelly` (o `header.jelly`):

```java
@Extension
public class MyChatDecorator extends PageDecorator {}
// No se necesita código Java — la plantilla hace todo el trabajo
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

## 🔐 Permisos

Jenkins tiene un sistema de permisos granular. Toda acción debe llamar a:

```java
Jenkins.get().checkPermission(Jenkins.READ);
```

Esto lanza una `AccessDeniedException` si el usuario actual no tiene el permiso requerido, que Stapler convierte automáticamente en una respuesta HTTP 403.

---

## 🗂️ Recursos Estáticos (webapp/)

Los archivos colocados en `src/main/webapp/` son servidos directamente por Jenkins en:
```
/plugin/<nombre-corto-del-plugin>/<ruta>
```

Por ejemplo, `src/main/webapp/copilot-chat.js` se sirve en `/plugin/copilot-chat/copilot-chat.js`.

---

## 📖 Más Información

- [Jenkins Plugin Developer Guide](https://www.jenkins.io/doc/developer/)
- [Documentación de Stapler](http://stapler.kohsuke.org/)
- [Índice de Extension Points de Jenkins](https://www.jenkins.io/doc/developer/extensions/)

# QaRobot

QaRobot es una herramienta de QA con interfaz Java/Swing y ejecución desde terminal. Envuelve Selenium para ejecutar flujos definidos en XML, permite elegir Chrome, Edge o Firefox, ejecuta verificaciones y genera reportes HTML y resultados JUnit para CI.

## Requisitos

- Windows 10/11 para la interfaz; el pipeline también comprueba la terminal en Linux.
- JDK 25 recomendado para compilar y ejecutar la distribución. El código del proyecto se compila a bytecode Java 17.
- Un navegador compatible: Chrome, Edge o Firefox.
- Node.js 20 o superior para reconstruir el dashboard o empaquetar una distribución; el pipeline usa Node.js 24. No se necesita Node para usar una distribución ya descargada.

Los drivers no necesitan copiarse al repositorio. Selenium Manager los localiza y los descarga la primera vez que se usa un navegador. Si se requiere un driver fijo, colócalo en `webdrivers/` dentro de la instalación de QaRobot (`chromedriver.exe`, `msedgedriver.exe` o `geckodriver.exe`).

## Ejecutar

Desde la raíz del repositorio:

```powershell
.\gradlew.bat :app:run
```

La aplicación crea su espacio de trabajo en `%USERPROFILE%\QaRobotWorkplace`. Para usar otra ubicación sin modificar el código:

```powershell
.\gradlew.bat :app:run -Pworkspace="C:\temp\qarobot-workspace"
```

El selector de la ventana permite elegir el navegador. `Chrome` es la opción inicial. La ventana puede abrirse desde cualquier directorio cuando se ejecuta con Gradle o desde la distribución generada; las plantillas del reporte se resuelven junto con la aplicación.

## Ejecutar desde terminal

Desde el repositorio, puedes ejecutar un escenario sin abrir la interfaz:

```powershell
.\gradlew.bat :app:run --args="run examples/offline-smoke.xml --browser chrome --output build/reportes --junit build/resultados.xml"
```

Desde la carpeta de una distribución descomprimida:

```powershell
.\bin\app.bat run examples\dynamic-waits.xml --output reportes --junit reportes\resultados.xml
.\bin\app.bat run --help
```

En Linux, usa `bash ./bin/app` en lugar de `.\bin\app.bat`. Sin argumentos se abre la GUI.

| Opción | Comportamiento |
| --- | --- |
| `run ARCHIVO_O_CARPETA [...]` | Ejecuta uno o más archivos o carpetas. Las carpetas aportan sus XML inmediatos en orden de nombre, sin recorrer subcarpetas. |
| `--browser chrome\|edge\|firefox` | Elige navegador; Chrome es el predeterminado. |
| `--headless` / `--headed` | Oculta o muestra el navegador. Por defecto se ejecuta sin ventana. |
| `--workspace CARPETA` | Ubicación del espacio de trabajo y las bibliotecas JavaScript. |
| `--output CARPETA` | Carpeta para reportes HTML y evidencias; cada escenario recibe una subcarpeta nueva. Por defecto: `<workspace>/dashboards`. |
| `--junit ARCHIVO` | Guarda un resultado JUnit XML conjunto con un caso por escenario. |

Las rutas relativas parten del directorio desde el que se invoca el comando. Cada XML usa una sesión nueva del navegador y la suite continúa aunque falle otro escenario. Cada archivo de una carpeta debe ser un escenario completo: si contiene fragmentos para `include`, indica explícitamente los XML principales.

La terminal devuelve `0` si todos los escenarios pasan, `1` si hay verificaciones fallidas o errores de ejecución y `2` si los argumentos son inválidos. Para integrar QaRobot con otras herramientas de CI, invoca directamente `bin/app` o `bin/app.bat`; Gradle puede convertir un fallo de la aplicación en su propio código de salida.

En modo terminal, `message` escribe en el log, `stop kill="false"` produce un error porque necesita intervención en la GUI y `stop kill="true"` conserva la terminación explícita del escenario. Los errores de JavaScript se propagan al resultado. Las rutas de archivos de propiedades en `<configuration>` se resuelven respecto al XML y un archivo ausente se informa como error.

## Ejemplo offline

`examples/offline-smoke.xml` contiene un flujo autocontenido: abre una página `data:`, escribe un nombre, pulsa un botón y valida el resultado. No depende de un sitio externo.

1. Ejecuta QaRobot.
2. Pulsa **Abrir XML…** y selecciona `examples/offline-smoke.xml`.
3. Elige el navegador y pulsa **Iniciar prueba**.

La primera ejecución de Selenium puede requerir acceso a internet para resolver el driver. Después, Selenium Manager usa su caché local.

También puedes abrir estos ejemplos desde la misma ventana:

- [`examples/dynamic-waits.xml`](examples/dynamic-waits.xml): espera campos editables, opciones que cargan después, botones habilitados y resultados visibles. Debe terminar correctamente.
- [`examples/failure-evidence.xml`](examples/failure-evidence.xml): provoca una verificación incorrecta y un tiempo de espera agotado para mostrar las capturas y el diagnóstico. **Debe terminar con error y generar dos evidencias.**

Los pasos y resultados esperados están en [`examples/README.md`](examples/README.md).

## Esperas de los elementos

Las acciones esperan el estado que necesitan antes de interactuar:

- `click`: elemento visible y habilitado.
- `write`: elemento visible, habilitado y sin `readonly`.
- `read`: elemento visible.
- `choose`: control visible y habilitado; en un `<select>` nativo también espera que exista y esté habilitada la opción solicitada.
- `verify`: presencia del elemento, para poder verificar también elementos ocultos o deshabilitados. La comprobación del valor se hace una vez; no se reintenta hasta que coincida.

El atributo opcional `waitTimeout` establece el máximo de espera en milisegundos. Todos los selectores alternativos de una acción comparten ese límite. Si no se indica, se conserva la espera global de la aplicación (`baseTimeout × 5`). Con `waitTimeout="0"` se comprueba el estado una vez.

`timeout` conserva su significado anterior: una pausa fija antes de ejecutar la acción. Usa `timeout="0"` para depender de la disponibilidad del elemento y evitar esa pausa:

```xml
<click id="guardar" desc="Guardar formulario" order="4" timeout="0" waitTimeout="5000">
    <selector by="ID">guardar</selector>
    <selector by="CSS">button[data-testid='guardar']</selector>
</click>
```

El clic o la escritura se ejecutan una sola vez después de la espera. Si el clic queda bloqueado por otro elemento, el fallo se informa en el reporte. Los desplegables personalizados de Vaadin conservan su navegación por teclado.

## Evidencia de fallos

Al fallar una acción o una verificación, QaRobot guarda una captura del navegador y un archivo de diagnóstico dentro de `evidence/`, en la carpeta del reporte de esa ejecución. No hace falta activar la grabación de video.

La sección **Diagnóstico de fallos** del resumen HTML muestra el paso, la hora, la URL, los selectores configurados y el último selector localizado, el error y los valores de las verificaciones fallidas. Incluye la captura y enlaces para descargarla junto con el diagnóstico; este último también contiene la traza de la excepción cuando existe.

La evidencia se toma antes de pasar a otra acción, también dentro de iteraciones. Si el navegador ya se cerró o no se puede escribir la captura, el reporte conserva el fallo original e indica qué evidencia no se pudo obtener. Las verificaciones fallidas permiten continuar con las siguientes acciones, pero el resultado final de la ejecución se marca como fallido.

## Pruebas

Pruebas unitarias y de integración sin navegador real:

```powershell
.\gradlew.bat test
```

Smoke del wrapper completo contra Chrome en modo headless:

```powershell
.\gradlew.bat :scraping:browserSmokeTest -Pbrowser=CHROME
```

También acepta `EDGE` o `FIREFOX`. El smoke se excluye de `test` para que la suite normal no dependa de un navegador instalado.

Smoke opcional de la ventana Swing:

```powershell
.\gradlew.bat :app:test -PguiSmoke=true
```

## Dashboard

El dashboard fuente está en `dashboardtemplate/`. Para instalar dependencias y generar sus archivos:

```powershell
cd dashboardtemplate
npm.cmd ci
npm.cmd run build
```

Al empaquetar, Gradle ejecuta `npm ci` y construye el dashboard automáticamente. Combina los artefactos de `dashboardtemplate/dist/` con la plantilla `app/dashboardtemplate/index.ftl`. La ejecución de desarrollo con `:app:run` usa la copia de `app/dashboardtemplate/`; actualízala si quieres probar allí cambios visuales del dashboard.

## Empaquetar

```powershell
.\gradlew.bat :app:installDist
.\gradlew.bat :app:distZip '-PreleaseVersion=2.1.0'
```

La instalación queda en `app/build/install/QaRobot/` y el ZIP en `app/build/distributions/QaRobot-2.1.0.zip`. Incluyen los lanzadores para Windows y Linux, las dependencias, el dashboard, los ejemplos y la documentación. La versión predeterminada sigue siendo `2.0`; `releaseVersion` permite indicar una versión como `2.1.0` o `2.1.0-rc.1`.

Para comprobar una instalación completa con PowerShell 7 y Chrome:

```powershell
.\.github\scripts\Test-Distribution.ps1 -DistributionPath app/build/install/QaRobot -OutputPath build/ci-smoke
```

El script ejecuta un escenario correcto y la carpeta completa de ejemplos. Comprueba los códigos de salida, HTML, JUnit, capturas y que la suite continúe después del fallo deliberado.

## GitHub Actions y Releases

El workflow [`.github/workflows/distribution.yml`](.github/workflows/distribution.yml) compila y prueba en Windows y Linux. Se ejecuta en pull requests, pushes a `main`, `master`, `devel` y `codex/**`, y manualmente desde Actions. Ejecuta las pruebas Java, los smoke tests con Chrome y una prueba de la distribución instalada. Conserva los reportes durante 14 días y el ZIP con su SHA-256 durante 30 días como artefactos descargables de Actions.

Un push de una etiqueta de versión publica ese ZIP en **GitHub Releases**, después de que ambos sistemas pasen las pruebas:

```powershell
git tag -a v2.1.0 -m "QaRobot 2.1.0: ejecución desde terminal y distribuciones automáticas"
git push origin v2.1.0
```

Crea la etiqueta sobre el commit que quieras distribuir, una vez subidos los cambios del workflow. La etiqueta determina la versión del ZIP. Una etiqueta como `v2.1.0-rc.1` crea una prerelease. La publicación incluye notas generadas por GitHub y `SHA256SUMS.txt`. El permiso de escritura se limita al job de publicación y usa el `GITHUB_TOKEN` del repositorio.

Si la release ya existe, el workflow compara por SHA-256 los archivos que ya tiene y sube únicamente los que faltan. Un reintento con los mismos archivos termina correctamente. Si un archivo del mismo nombre tiene contenido diferente, la publicación se detiene antes de subir archivos; usa una nueva etiqueta para distribuir contenido distinto. Se conservan el título, las notas y el estado de una release existente. Una release inmutable que tenga archivos pendientes debe completarse como una nueva versión.

Para completar una release creada previamente o publicar una etiqueta con el workflow actualizado:

1. Sube esta versión del workflow y asegúrate de que también esté disponible en la rama predeterminada del repositorio, para habilitar **Run workflow**.
2. En **Actions → Build, test and distribute → Run workflow**, elige una rama que contenga el workflow corregido.
3. Escribe la etiqueta existente, por ejemplo `v2.2.0-op.1`, en **release_tag**.

La ejecución manual usa las herramientas de publicación de la rama seleccionada y compila el commit de la etiqueta, fijado una sola vez para Windows y Linux. Si dejas **release_tag** vacío, solo construye la referencia seleccionada y guarda artefactos. **Re-run jobs** sobre una ejecución anterior conserva el workflow de aquella ejecución; para incorporar correcciones, inicia una ejecución manual nueva.

Las pruebas del publicador usan respuestas simuladas de GitHub y se pueden ejecutar localmente sin credenciales ni publicaciones:

```powershell
python -m unittest discover -s .github/scripts -p test_publish_distribution.py -v
```

## Dependencias y apariencia

Los scripts de compilación usan Kotlin DSL (`build.gradle.kts` y `settings.gradle.kts`). Los plugins de convenciones compartidos están en `buildSrc/src/main/kotlin/`.

Las versiones de las dependencias Java y del plugin Node de Gradle se administran en `gradle/libs.versions.toml`. Los módulos usan aliases `libs.*`; `buildSrc` importa el mismo catálogo para sus plugins de convenciones. Las dependencias npm del dashboard se mantienen en su `package.json` y lockfile.

Jackson usa la versión 3.1.7, alineada con su BOM. Los mappers JSON, XML y YAML y el adaptador de Unirest usan `tools.jackson`. El paquete `com.fasterxml.jackson.annotation` permanece porque Jackson 3 comparte ese módulo de anotaciones; no se incluye Jackson 2 core/databind. El esquema de los escenarios XML sigue procesándose mediante JAXB.

La interfaz usa FlatLaf y toma automáticamente el tema claro u oscuro del sistema operativo, tanto al abrirse como cuando cambia la preferencia del sistema. En Windows sigue el modo de las aplicaciones. La ventana principal, la tabla de acciones y los diálogos comparten el tema; las preferencias antiguas de `ui.properties` ya no se usan. Si el sistema no permite detectar el tema, se usa el claro. Los layouts se mantienen en las clases Java de `app/ui`; los archivos `.jfd` se conservan como referencia del diseño anterior y ya no generan estas ventanas.

## Estructura

- `commons`: modelos, esquema XML y utilidades compartidas.
- `scraping`: Selenium, acciones XML, interpolación y scripts GraalJS.
- `record`: grabación de pantalla.
- `app`: interfaz Swing, comandos de terminal y orquestación.
- `dashboardtemplate`: fuente SCSS/JavaScript del reporte.

El modelo Java del XML se genera durante la compilación desde `commons/src/main/resources/com/rebirth/qarobot/commons/xsd/qarobot_v2.xsd`; no es necesario instalar JAXB ni ejecutar una tarea adicional.

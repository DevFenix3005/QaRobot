# QaRobot

QaRobot es una herramienta de QA de escritorio construida en Java/Swing. Envuelve Selenium para ejecutar flujos definidos en XML, permite elegir Chrome, Edge o Firefox, ejecuta verificaciones y genera un reporte HTML con los resultados.

## Requisitos

- Windows 10/11 (la interfaz es Swing; el código también conserva rutas portables para otros sistemas).
- JDK 17 o superior. El proyecto compila a bytecode Java 17; JDK 25 funciona con el wrapper incluido.
- Un navegador compatible: Chrome, Edge o Firefox.
- Node.js 20 o superior solo si se va a reconstruir el dashboard.

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

La aplicación usa la copia distribuible de `app/dashboardtemplate/`; después de cambiar el fuente, copia los artefactos generados allí antes de empaquetar una distribución.

## Empaquetar

```powershell
.\gradlew.bat :app:installDist
```

La distribución queda en `app/build/install/QaRobot/`. Su ejecutable está en `app/build/install/QaRobot/bin/app.bat`.

## Dependencias y apariencia

Los scripts de compilación usan Kotlin DSL (`build.gradle.kts` y `settings.gradle.kts`). Los plugins de convenciones compartidos están en `buildSrc/src/main/kotlin/`.

Las versiones de las dependencias Java y del plugin Node de Gradle se administran en `gradle/libs.versions.toml`. Los módulos usan aliases `libs.*`; `buildSrc` importa el mismo catálogo para sus plugins de convenciones. Las dependencias npm del dashboard se mantienen en su `package.json` y lockfile.

Jackson usa la versión 3.1.7, alineada con su BOM. Los mappers JSON, XML y YAML y el adaptador de Unirest usan `tools.jackson`. El paquete `com.fasterxml.jackson.annotation` permanece porque Jackson 3 comparte ese módulo de anotaciones; no se incluye Jackson 2 core/databind. El esquema de los escenarios XML sigue procesándose mediante JAXB.

La interfaz usa FlatLaf y toma automáticamente el tema claro u oscuro del sistema operativo, tanto al abrirse como cuando cambia la preferencia del sistema. En Windows sigue el modo de las aplicaciones. La ventana principal, la tabla de acciones y los diálogos comparten el tema; las preferencias antiguas de `ui.properties` ya no se usan. Si el sistema no permite detectar el tema, se usa el claro. Los layouts se mantienen en las clases Java de `app/ui`; los archivos `.jfd` se conservan como referencia del diseño anterior y ya no generan estas ventanas.

## Estructura

- `commons`: modelos, esquema XML y utilidades compartidas.
- `scraping`: Selenium, acciones XML, interpolación y scripts GraalJS.
- `record`: grabación de pantalla.
- `app`: interfaz Swing y orquestación.
- `dashboardtemplate`: fuente SCSS/JavaScript del reporte.

El modelo Java del XML se genera durante la compilación desde `commons/src/main/resources/com/rebirth/qarobot/commons/xsd/qarobot_v2.xsd`; no es necesario instalar JAXB ni ejecutar una tarea adicional.

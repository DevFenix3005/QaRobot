# QaRobot

Herramienta de QA para ejecutar escenarios XML con Selenium, desde la interfaz gráfica o la terminal.

## Requisitos

Instala Chrome, Edge o Firefox. El instalador de Windows incluye Java; para usar el ZIP portable instala además JDK 25 y configura `JAVA_HOME` o deja `java` disponible en `PATH`. Selenium Manager obtiene el driver del navegador; la primera ejecución puede necesitar internet. La distribución ya incluye las bibliotecas y el dashboard.

## Instalación en Windows

Ejecuta el instalador `QaRobot-VERSION-windows-x64.exe`. Después, abre **QaRobot** desde el menú Inicio o desde `QaRobot.exe` en la carpeta de instalación. Puedes desinstalarlo desde las aplicaciones instaladas de Windows.

Para usar la terminal desde la carpeta de instalación:

```powershell
.\QaRobot-cli.exe run .\app\examples\offline-smoke.xml
.\QaRobot-cli.exe --help
```

En esta instalación, los ejemplos están en `app/examples/` y la documentación en `app/docs/`. Los datos del usuario se guardan por defecto en `%USERPROFILE%\QaRobotWorkplace` y los logs en su subcarpeta `logs`.

## Interfaz gráfica del ZIP portable

En Windows, ejecuta `bin/app.bat`. En Linux, ejecuta `bash ./bin/app` desde una sesión de escritorio.

## Terminal del ZIP portable

Desde esta carpeta, en PowerShell:

```powershell
.\bin\app.bat run examples\offline-smoke.xml --browser chrome --output reportes --junit reportes\resultados.xml
.\bin\app.bat run --help
```

En Linux:

```sh
bash ./bin/app run examples/offline-smoke.xml --browser chrome --output reportes --junit reportes/resultados.xml
```

Por defecto, el navegador se ejecuta sin ventana; agrega `--headed` para mostrarlo. Puedes pasar varios XML o una carpeta con escenarios completos. Los reportes se guardan en subcarpetas nuevas y los errores incluyen evidencia cuando está disponible.

Códigos de salida: `0` todos correctos, `1` pruebas fallidas o errores de ejecución, `2` argumentos inválidos.

`examples/dynamic-waits.xml` debe pasar. `examples/failure-evidence.xml` falla deliberadamente y genera dos evidencias; por ello, ejecutar toda la carpeta `examples` devuelve `1`.

Consulta `docs/README.md` para ver todas las opciones, la configuración y el formato de los escenarios. `examples/README.md` describe los ejemplos incluidos.

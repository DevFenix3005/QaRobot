# QaRobot

Herramienta de QA para ejecutar escenarios XML con Selenium, desde la interfaz gráfica o la terminal.

## Requisitos

Instala JDK 25 y Chrome, Edge o Firefox. Configura `JAVA_HOME` o deja `java` disponible en `PATH`. Selenium Manager obtiene el driver del navegador; la primera ejecución puede necesitar internet. La distribución ya incluye las bibliotecas y el dashboard.

## Interfaz gráfica

En Windows, ejecuta `bin/app.bat`. En Linux, ejecuta `bash ./bin/app` desde una sesión de escritorio.

## Terminal

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

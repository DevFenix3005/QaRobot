# Ejemplos de QaRobot

Ejecuta `./gradlew.bat :app:run` desde la raíz del proyecto. En la ventana, pulsa **Abrir XML…**, elige uno de estos archivos y pulsa **Iniciar prueba**.

Todos incluyen su propia página en una URL `data:`: no necesitan servidor ni un sitio externo. La primera descarga del driver puede requerir internet.

| Archivo | Qué muestra | Resultado esperado |
| --- | --- | --- |
| [offline-smoke.xml](offline-smoke.xml) | Un formulario sencillo con escritura, clic y verificación. | Correcto. |
| [dynamic-waits.xml](dynamic-waits.xml) | Controles que se preparan con retraso y selectores alternativos. | Correcto, sin evidencias de fallo. |
| [failure-evidence.xml](failure-evidence.xml) | Una verificación incorrecta y un clic que agota su espera. | **Fallido intencionalmente**, con dos evidencias. |

## Esperas de controles dinámicos

`dynamic-waits.xml` prepara cada control 3.5 segundos después de la interacción anterior:

1. Pulsa **Iniciar demo**.
2. Espera a que el campo de nombre sea visible, esté habilitado y deje de ser de solo lectura; escribe `QaRobot`.
3. Espera a que aparezca la opción **Pruebas** y la selecciona.
4. Espera a que **Guardar** esté habilitado. El primer selector (`guardar-antiguo`) no existe; el segundo (`#guardar`) encuentra el botón.
5. Espera a que aparezca el resultado, lo guarda como `resultado_obtenido` y verifica `READY_QaRobot_Pruebas`.

Las acciones usan `timeout="0"` para evitar pausas fijas y `waitTimeout="8000"` como límite de espera del control. Los avisos informativos de la GUI pueden añadir tiempo entre pasos. La verificación final comprueba el valor una sola vez, después de que la lectura esperó la aparición del resultado.

## Evidencia de fallos

`failure-evidence.xml` debe terminar con error. Los dos fallos son parte de la demostración:

1. **`fallo-verificacion`:** espera `APROBADO`, pero la página muestra `PENDIENTE`. Guarda una captura y continúa con los siguientes pasos.
2. Pulsa **Continuar** y comprueba que la página cambió a `CONTINUO_DESPUES_DEL_FALLO`.
3. **`fallo-timeout`:** intenta pulsar `boton-inexistente` durante un máximo de 1200 ms. Guarda otra captura y detiene la ejecución.

En el reporte HTML, abre **Resumen → Diagnóstico de fallos**. Verás el paso, la URL, los selectores, el error y las capturas. La primera evidencia incluye el valor esperado y el obtenido; la segunda incluye el error de espera.

Junto a `index.html`, la carpeta `evidence/` contendrá `failure-0001.png`, `failure-0001.txt`, `failure-0002.png` y `failure-0002.txt`. También puedes descargarlos desde el reporte. No hace falta activar la grabación de video.

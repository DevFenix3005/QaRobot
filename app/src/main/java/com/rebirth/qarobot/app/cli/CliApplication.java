package com.rebirth.qarobot.app.cli;

import com.rebirth.qarobot.app.di.DaggerCliComponent;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;

/** Testable entry point; only Main translates the returned status into System.exit. */
public final class CliApplication {
    private static final String HELP = """
            QaRobot - ejecución de escenarios desde la terminal

            Uso:
              app run ARCHIVO_O_CARPETA [...] [opciones]
              app --help

            Opciones:
              --browser chrome|edge|firefox  Navegador (predeterminado: chrome).
              --headless                    Ejecutar sin ventana (predeterminado).
              --headed                      Mostrar la ventana del navegador.
              --workspace CARPETA           Espacio de trabajo y bibliotecas JavaScript.
              --output CARPETA              Guardar cada reporte en una subcarpeta nueva.
              --junit ARCHIVO               Guardar el resultado conjunto como JUnit XML.
              --help, -h                    Mostrar esta ayuda.

            Las carpetas incluyen sus XML inmediatos, ordenados por nombre; no son recursivas.
            Cada XML debe ser un escenario completo. Para usar fragmentos, pasa los archivos
            principales explícitamente. Cada escenario utiliza una sesión nueva del navegador.
            Las rutas relativas parten del directorio actual. Sin --output, los reportes se
            guardan en <workspace>/dashboards. Sin argumentos se abre la interfaz gráfica.

            Códigos de salida: 0 correcto, 1 prueba/error de ejecución, 2 argumentos inválidos.
            """;

    private final Function<CliOptions, ScenarioExecutor> executorFactory;

    public CliApplication() {
        this(options -> {
            var component = DaggerCliComponent.factory().create(999);
            return new XmlScenarioExecutor(component.xmlReader(), component.scrapingFactory(), options.browser());
        });
    }

    CliApplication(Function<CliOptions, ScenarioExecutor> executorFactory) {
        this.executorFactory = executorFactory;
    }

    public int execute(String[] args, PrintStream out, PrintStream err) {
        if (args.length > 0 && ("run".equals(args[0]) || args.length == 1)
                && Arrays.stream(args).anyMatch(arg -> "--help".equals(arg) || "-h".equals(arg))) {
            out.print(HELP);
            return 0;
        }
        CliOptions options;
        try {
            options = CliOptions.parse(args, Path.of(System.getProperty("user.dir")));
        } catch (Exception failure) {
            err.println("Argumentos inválidos: " + failure.getMessage());
            err.println("Consulta 'app run --help' para ver las opciones.");
            return 2;
        }
        System.setProperty("qarobot.cli", "true");
        System.setProperty("qarobot.headless", Boolean.toString(options.headless()));
        System.setProperty("qarobot.openReport", "false");
        System.setProperty("qarobot.workspace", options.workspace().toString());
        try {
            // Resolve the graph once, but defer construction until a scenario is executed so
            // initialization failures are recorded in the same suite/JUnit result as other errors.
            ScenarioExecutor lazy = new ScenarioExecutor() {
                private ScenarioExecutor delegate;

                @Override
                public ScenarioResult execute(Path scenario, Path directory) throws Exception {
                    if (delegate == null) delegate = executorFactory.apply(options);
                    return delegate.execute(scenario, directory);
                }
            };
            return new CliSuiteRunner(lazy, out, err).run(options);
        } catch (Exception failure) {
            err.println("No se pudo completar la ejecución: " + failure.getMessage());
            return 1;
        }
    }
}

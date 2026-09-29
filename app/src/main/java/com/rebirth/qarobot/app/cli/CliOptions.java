package com.rebirth.qarobot.app.cli;

import com.rebirth.qarobot.scraping.enums.Browser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Validated command-line input. Parsing never starts the browser or initializes Swing. */
public record CliOptions(List<Path> scenarios, Browser browser, boolean headless,
                         Path workspace, Path output, Path junit) {

    public CliOptions {
        scenarios = List.copyOf(scenarios);
    }

    public static CliOptions parse(String[] args, Path workingDirectory) throws IOException {
        if (args.length == 0 || !"run".equals(args[0])) {
            throw new IllegalArgumentException("El comando debe comenzar con 'run'.");
        }
        Browser browser = Browser.CHROME;
        boolean headless = true;
        Path workspace = resolve(workingDirectory,
                System.getProperty("qarobot.workspace", Path.of(System.getProperty("user.home"), "QaRobotWorkplace").toString()));
        Path output = null;
        Path junit = null;
        List<Path> inputs = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        boolean positionalOnly = false;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if ("--".equals(arg) && !positionalOnly) {
                positionalOnly = true;
            } else if (arg.startsWith("-") && !positionalOnly) {
                String key = ("--headless".equals(arg) || "--headed".equals(arg)) ? "browser-display" : arg;
                if (!seen.add(key)) throw new IllegalArgumentException("Opción repetida o contradictoria: " + arg);
                switch (arg) {
                    case "--headless" -> headless = true;
                    case "--headed" -> headless = false;
                    case "--browser", "--workspace", "--output", "--junit" -> {
                        if (++i >= args.length || args[i].startsWith("--")) {
                            throw new IllegalArgumentException("Falta el valor de " + arg);
                        }
                        String value = args[i];
                        switch (arg) {
                            case "--browser" -> {
                                try {
                                    browser = Browser.valueOf(value.toUpperCase(Locale.ROOT));
                                } catch (IllegalArgumentException failure) {
                                    throw new IllegalArgumentException("Navegador inválido: " + value + ". Usa chrome, edge o firefox.");
                                }
                            }
                            case "--workspace" -> workspace = resolve(workingDirectory, value);
                            case "--output" -> output = resolve(workingDirectory, value);
                            case "--junit" -> junit = resolve(workingDirectory, value);
                        }
                    }
                    default -> throw new IllegalArgumentException("Opción desconocida: " + arg);
                }
            } else {
                inputs.add(resolve(workingDirectory, arg));
            }
        }
        if (inputs.isEmpty()) throw new IllegalArgumentException("Indica al menos un archivo XML o una carpeta de escenarios.");
        LinkedHashSet<Path> scenarios = new LinkedHashSet<>();
        for (Path input : inputs) {
            if (Files.isDirectory(input)) {
                List<Path> files;
                try (var children = Files.list(input)) {
                    files = children.filter(Files::isRegularFile).filter(CliOptions::isXml)
                            .sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
                }
                if (files.isEmpty()) throw new IllegalArgumentException("La carpeta no contiene escenarios XML: " + input);
                for (Path file : files) addScenario(scenarios, file);
            } else {
                addScenario(scenarios, input);
            }
        }
        if (output == null) output = workspace.resolve("dashboards");
        checkDirectoryDestination(workspace);
        checkDirectoryDestination(output);
        if (junit != null) {
            if (Files.exists(junit) && (!Files.isRegularFile(junit) || !Files.isWritable(junit))) {
                throw new IllegalArgumentException("La salida JUnit debe ser un archivo escribible: " + junit);
            }
            checkDirectoryDestination(junit.getParent());
            for (Path scenario : scenarios) {
                if (scenario.equals(junit) || (Files.exists(junit) && Files.isSameFile(scenario, junit))) {
                    throw new IllegalArgumentException("La salida JUnit no puede reemplazar un escenario de entrada: " + junit);
                }
            }
        }
        return new CliOptions(new ArrayList<>(scenarios), browser, headless, workspace, output, junit);
    }

    private static Path resolve(Path directory, String value) {
        if (value.isBlank()) throw new IllegalArgumentException("Las rutas no pueden estar vacías.");
        try {
            return directory.resolve(value).toAbsolutePath().normalize();
        } catch (InvalidPathException failure) {
            throw new IllegalArgumentException("Ruta inválida: " + value, failure);
        }
    }

    private static void addScenario(Set<Path> scenarios, Path input) throws IOException {
        if (!Files.isRegularFile(input) || !Files.isReadable(input) || !isXml(input)) {
            throw new IllegalArgumentException("El escenario debe ser un archivo XML legible: " + input);
        }
        scenarios.add(input.toRealPath());
    }

    private static boolean isXml(Path input) {
        return input.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml");
    }

    private static void checkDirectoryDestination(Path directory) {
        Path parent = directory;
        while (parent != null && !Files.exists(parent)) parent = parent.getParent();
        if (parent == null || !Files.isDirectory(parent) || !Files.isWritable(parent)) {
            throw new IllegalArgumentException("No se puede escribir en la carpeta: " + directory);
        }
    }
}

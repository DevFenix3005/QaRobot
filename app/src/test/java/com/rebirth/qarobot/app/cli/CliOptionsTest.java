package com.rebirth.qarobot.app.cli;

import com.rebirth.qarobot.scraping.enums.Browser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CliOptionsTest {
    @TempDir Path temp;

    @Test
    void expandsDirectoriesInNameOrderWithoutRecursionAndDeduplicatesFiles() throws Exception {
        Path directory = Files.createDirectory(temp.resolve("escenarios"));
        Path z = Files.writeString(directory.resolve("z.XML"), "<qarobot/>");
        Path a = Files.writeString(directory.resolve("a.xml"), "<qarobot/>");
        Files.writeString(directory.resolve("notes.txt"), "not a scenario");
        Files.createDirectory(directory.resolve("nested"));
        Files.writeString(directory.resolve("nested/ignored.xml"), "<qarobot/>");
        var options = parse("run", "escenarios", "escenarios/a.xml");
        assertEquals(List.of(a.toRealPath(), z.toRealPath()), options.scenarios());
        assertEquals(Browser.CHROME, options.browser());
        assertTrue(options.headless());
    }

    @Test
    void resolvesOptionsAgainstCallerWorkingDirectory() throws Exception {
        Files.writeString(temp.resolve("test.xml"), "<qarobot/>");
        var options = parse("run", "--browser", "FIREFOX", "test.xml", "--headed", "--workspace", "my work",
                "--output", "results", "--junit", "reports/suite.xml");
        assertEquals(Browser.FIREFOX, options.browser());
        assertFalse(options.headless());
        assertEquals(temp.resolve("my work"), options.workspace());
        assertEquals(temp.resolve("results"), options.output());
        assertEquals(temp.resolve("reports/suite.xml"), options.junit());
    }

    @Test
    void defaultsReportsToSelectedWorkspace() throws Exception {
        Files.writeString(temp.resolve("test.xml"), "<qarobot/>");
        var options = parse("run", "test.xml", "--workspace", "work");
        assertEquals(temp.resolve("work/dashboards"), options.output());
    }

    @Test
    void rejectsInvalidCommandsUnknownOptionsMissingValuesAndConflictingModes() throws Exception {
        Files.writeString(temp.resolve("test.xml"), "<qarobot/>");
        for (String[] args : List.of(new String[]{"unknown"}, new String[]{"run"},
                new String[]{"run", "test.xml", "--bogus"}, new String[]{"run", "test.xml", "--browser"},
                new String[]{"run", "test.xml", "--browser", "safari"},
                new String[]{"run", "test.xml", "--headless", "--headed"},
                new String[]{"run", "test.xml", "--output", "--headless"})) {
            assertThrows(IllegalArgumentException.class, () -> parse(args), String.join(" ", args));
        }
    }

    @Test
    void rejectsMissingInputNonXmlEmptyDirectoryAndFileUsedAsOutputDirectory() throws Exception {
        Files.writeString(temp.resolve("test.xml"), "<qarobot/>");
        Files.writeString(temp.resolve("notes.txt"), "text");
        Files.createDirectory(temp.resolve("empty"));
        assertThrows(IllegalArgumentException.class, () -> parse("run", "missing.xml"));
        assertThrows(IllegalArgumentException.class, () -> parse("run", "notes.txt"));
        assertThrows(IllegalArgumentException.class, () -> parse("run", "empty"));
        assertThrows(IllegalArgumentException.class, () -> parse("run", "test.xml", "--output", "notes.txt/reports"));
        assertThrows(IllegalArgumentException.class, () -> parse("run", "test.xml", "--junit", "test.xml"));
    }

    @Test
    void validatesEveryInputBeforeReturning() throws Exception {
        Files.writeString(temp.resolve("first.xml"), "<qarobot/>");
        assertThrows(IllegalArgumentException.class, () -> parse("run", "first.xml", "missing.xml"));
    }

    private CliOptions parse(String... args) throws Exception {
        return CliOptions.parse(args, temp);
    }
}

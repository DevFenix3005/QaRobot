package com.rebirth.qarobot.app.cli;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

final class JunitReport {
    private JunitReport() {}

    static void write(Path target, List<ScenarioResult> results) throws IOException, XMLStreamException {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".qarobot-junit-", ".xml");
        try {
            try (Writer output = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
                xml.writeStartDocument("UTF-8", "1.0");
                xml.writeStartElement("testsuite");
                xml.writeAttribute("name", "QaRobot");
                xml.writeAttribute("tests", Integer.toString(results.size()));
                xml.writeAttribute("failures", Long.toString(count(results, ScenarioResult.Status.FAILURE)));
                xml.writeAttribute("errors", Long.toString(count(results, ScenarioResult.Status.ERROR)));
                xml.writeAttribute("skipped", "0");
                xml.writeAttribute("time", Double.toString(results.stream().mapToDouble(ScenarioResult::seconds).sum()));
                for (ScenarioResult result : results) {
                    xml.writeStartElement("testcase");
                    xml.writeAttribute("classname", "qarobot.scenarios");
                    xml.writeAttribute("name", safe(result.scenario().toString()));
                    xml.writeAttribute("time", Double.toString(result.seconds()));
                    if (result.status() != ScenarioResult.Status.PASSED) {
                        xml.writeStartElement(result.status() == ScenarioResult.Status.FAILURE ? "failure" : "error");
                        xml.writeAttribute("message", safe(result.message()));
                        xml.writeAttribute("type", safe(result.errorType()));
                        xml.writeCharacters(safe(result.message()));
                        xml.writeEndElement();
                    }
                    if (result.report() != null) {
                        xml.writeStartElement("system-out");
                        xml.writeCharacters("Reporte: " + safe(result.report().toString()));
                        xml.writeEndElement();
                    }
                    xml.writeEndElement();
                }
                xml.writeEndElement();
                xml.writeEndDocument();
                xml.close();
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static long count(List<ScenarioResult> results, ScenarioResult.Status status) {
        return results.stream().filter(result -> result.status() == status).count();
    }

    private static String safe(String value) {
        if (value == null) return "";
        StringBuilder clean = new StringBuilder();
        value.codePoints().filter(code -> code == 9 || code == 10 || code == 13
                || (code >= 0x20 && code <= 0xD7FF) || (code >= 0xE000 && code <= 0xFFFD)
                || (code >= 0x10000 && code <= 0x10FFFF)).forEach(clean::appendCodePoint);
        return clean.toString();
    }
}

package com.rebirth.qarobot.app.di.modules;

import com.rebirth.qarobot.app.utils.QaXmlReadService;
import com.rebirth.qarobot.commons.di.enums.PatternEnum;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.models.dtos.QarobotWrapper;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ClickActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.OpenActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.VerifyActionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class OfflineExampleTest {
    @TempDir Path temp;

    @Test
    void readsAndValidatesThePortableOfflineExample() throws Exception {
        QaXmlReadService reader = reader();
        Path example = Path.of("../examples/offline-smoke.xml").toAbsolutePath().normalize();
        reader.setQaXmlFile(example.toFile());

        QarobotWrapper robot = reader.read();
        try {
            assertTrue(robot.isValidXml(), () -> String.valueOf(robot.getErrores()));
            assertEquals(6, robot.getActions().size());
            assertEquals("QaRobot", robot.getConfiguration().getSet().get(0).getValue());
            OpenActionType open = (OpenActionType) robot.getActions().get(0);
            assertTrue(open.getUrl().startsWith("data:text/html"));
            assertTrue(robot.getActions().stream().allMatch(action -> action.getId() != null));
            assertEquals(temp.toFile(), robot.getDashboardExitFile().getParentFile());
        } finally {
            Files.deleteIfExists(robot.getXmlTempFile().toPath());
        }
    }

    @Test
    void readsOptionalWaitTimeoutWithoutChangingLegacyActionDelays() throws Exception {
        Path scenario = temp.resolve("wait-timeouts.xml");
        Files.writeString(scenario, """
                <qarobot xmlns="https://www.qarobot.rebirth.com.mx">
                    <configuration/>
                    <click desc="Wait for button" order="1" timeout="125" waitTimeout="1500">
                        <selector by="ID">save</selector>
                    </click>
                    <verify desc="Inspect immediately" order="2" timeout="250" waitTimeout="0" negative="true">
                        <selector by="ID">hidden</selector>
                    </verify>
                    <click desc="Legacy click" order="3" timeout="375">
                        <selector by="ID">legacy-button</selector>
                    </click>
                    <verify desc="Legacy verification" order="4" timeout="500" value="Ready">
                        <selector by="ID">legacy-result</selector>
                    </verify>
                </qarobot>
                """);
        QaXmlReadService reader = reader();
        reader.setQaXmlFile(scenario.toFile());

        QarobotWrapper robot = reader.read();
        try {
            assertTrue(robot.isValidXml(), () -> String.valueOf(robot.getErrores()));
            assertEquals(4, robot.getActions().size());
            ClickActionType click = assertInstanceOf(ClickActionType.class, robot.getActions().get(0));
            VerifyActionType verify = assertInstanceOf(VerifyActionType.class, robot.getActions().get(1));
            ClickActionType legacyClick = assertInstanceOf(ClickActionType.class, robot.getActions().get(2));
            VerifyActionType legacyVerify = assertInstanceOf(VerifyActionType.class, robot.getActions().get(3));

            assertEquals(BigInteger.valueOf(1500), click.getWaitTimeout());
            assertEquals(BigInteger.ZERO, verify.getWaitTimeout());
            assertNull(legacyClick.getWaitTimeout());
            assertNull(legacyVerify.getWaitTimeout());
            assertEquals(BigInteger.valueOf(125), click.getTimeout());
            assertEquals(BigInteger.valueOf(250), verify.getTimeout());
            assertEquals(BigInteger.valueOf(375), legacyClick.getTimeout());
            assertEquals(BigInteger.valueOf(500), legacyVerify.getTimeout());
        } finally {
            Files.deleteIfExists(robot.getXmlTempFile().toPath());
        }
    }

    private QaXmlReadService reader() throws Exception {
        Configuracion configuration = new Configuracion();
        configuration.setDashboardsOutputs(temp.toFile());
        return new QaXmlReadService(XmlReaderModule.documentBuilderProvider(), configuration,
                AppModule.loremProvider(), new Random(1), Map.of(
                PatternEnum.STRING_PATTERN, PatternsModule.randomStringPattern(),
                PatternEnum.INTEGER_PATTERN, PatternsModule.randomIntegerPattern()));
    }
}

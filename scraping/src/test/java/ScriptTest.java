import lombok.extern.log4j.Log4j2;
import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.*;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.scraping.di.modules.ScriptModule;
import com.rebirth.qarobot.scraping.models.qabot.rhinox.Rhinox;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScriptTest {
    private ScriptEngine graalEngine;

    @BeforeAll
    public void startUp() {
        Configuracion configuracion = new Configuracion();
        configuracion.setScriptsHome(new File("C:\\Users\\fenix\\QaRobotWorkplace\\scripts"));
        ScriptEngineManager scriptManager = ScriptModule.contextProvide();
        graalEngine = ScriptModule.scopeProvide(scriptManager, configuracion);
    }

    @Test()
    @Order(100)
    @DisplayName("Prueba con graal!")
    void graalJsTest() {
        Rhinox rhinox = new Rhinox(graalEngine);
        rhinox.addProperties2Scope("x", 12);
        rhinox.addProperties2Scope("y", 12);
        rhinox.addProperties2Scope("x", 15);
        rhinox.addProperties2Scope("y", 15);

        Callable<Integer> runnable = () -> {
            try {
                String script = """
                            let { assert } = self.chai;
                            let f = _.now();
                            let v = 12;
                            let w = 12;
                            let z = _.sum([v, w, x, y]);
                            assert.equal(z, 54);
                            return z;
                        """;
                Object resultado = rhinox.runScript(UUID.randomUUID().toString(), script);
                return (int) resultado;
            } catch (ScriptException e) {
                if (e.getCause() instanceof PolyglotException polyglotException) {
                    String message = polyglotException.getMessage();
                    System.out.println(message);
                }
                Assertions.fail();
            }
            return 0;
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 10; i++) {
                int resultado = executor.submit(runnable).get();
                System.out.println(resultado);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

    }

}

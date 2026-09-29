package com.rebirth.qarobot.scraping.models.qabot.rhinox;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Map;
import java.util.Objects;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import com.rebirth.qarobot.commons.models.dtos.qarobot.VerifyActionType;

@Slf4j
@Data
public final class Rhinox {

    private final ScriptEngine scriptEngine;

    public Rhinox(ScriptEngine scriptEngine) {
        this.scriptEngine = Objects.requireNonNull(scriptEngine, "scriptEngine");
    }

    public void addProperties2Scope(Map<String, Object> properties) {
        synchronized (scriptEngine) {
            properties.forEach(this::addProperties2Scope);
        }
    }

    public void addProperties2Scope(String key, Object value) {
        synchronized (scriptEngine) {
            scriptEngine.put(key, value);
        }
    }

    public Object runScript(VerifyActionType verifyActionDto) throws ScriptException {
        String id = verifyActionDto.getId();
        String verifyScript = verifyActionDto.getScript();
        return this.runScript(id, verifyScript);
    }

    public Object runScript(String id, String verifyScript) throws ScriptException {
        Objects.requireNonNull(verifyScript, "verifyScript");
        log.info("Corriendo prueba con id: {}", id);
        String iifeScript = "(function(){\n" + verifyScript + "\n})();";
        // A single GraalJS context must not be entered concurrently by different wrappers.
        synchronized (scriptEngine) {
            return scriptEngine.eval(iifeScript);
        }
    }

}

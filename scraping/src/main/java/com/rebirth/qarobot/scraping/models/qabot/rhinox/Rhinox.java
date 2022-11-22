package com.rebirth.qarobot.scraping.models.qabot.rhinox;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Map;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import com.rebirth.qarobot.commons.models.dtos.qarobot.VerifyActionType;

@Slf4j
@Data
public final class Rhinox {

    private final ScriptEngine scriptEngine;

    public Rhinox(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    public void addProperties2Scope(Map<String, Object> properties) {
        properties.forEach(this::addProperties2Scope);
    }

    public void addProperties2Scope(String key, Object value) {
        scriptEngine.put(key, value);
    }

    public Object runScript(VerifyActionType verifyActionDto) throws ScriptException {
        String id = verifyActionDto.getId();
        String verifyScript = verifyActionDto.getScript();
        return this.runScript(id, verifyScript);
    }

    public Object runScript(String id, String verifyScript) throws ScriptException {
        log.info("Corriendo pueba con id: {}", id);
        String iifeScript = "(function(){" + verifyScript + "})();";
        return scriptEngine.eval(iifeScript);
    }

}

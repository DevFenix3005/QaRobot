package com.rebirth.qarobot.commons.models.dtos;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.google.common.collect.Lists;
import lombok.Data;
import com.rebirth.qarobot.commons.models.dtos.qarobot.VerifyActionType;

import java.util.ArrayList;
import java.util.List;

@Data
public class Verificador {

    private String id;
    private String descVerifyAccion;
    private String rule;
    private String script;
    private String evaluado;
    private String resultado;
    private boolean ok;
    private boolean skip;
    private List<Verificador> verificadorList;


    public static Verificador create(String id, String desc) {
        Verificador verificador = new Verificador();
        verificador.setId(id);
        verificador.setDescVerifyAccion(desc);
        return verificador;
    }

    public static Verificador create(VerifyActionType verifyActionType) {
        return create(verifyActionType.getId(), verifyActionType.getDesc());
    }

    @Override
    public String toString() {
        return AsciiTable.getTable(Lists.newArrayList(this), Lists.newArrayList(
                new Column().header("Regla").with(Verificador::getRule),
                new Column().header("Valor Evaluado").with(Verificador::getEvaluado),
                new Column().header("Valor De Comparacion").with(Verificador::getResultado),
                new Column().header("Resultado").with(ver -> ver.isOk() ? "Paso la verificacion" : "No paso la verificacion")
                //new Column().header("Script").with(Verificador::getScript)
        ));
    }

    public String getResultadoEvaluacion() {
        return isOk() ? "Paso la verificacion" : "No paso la verificacion";
    }


    public void addVerificacion(Verificador verificador) {
        if (verificadorList == null) {
            this.ok = true;
            this.rule = "";
            this.verificadorList = new ArrayList<>();
        }
        this.ok &= verificador.ok;
        this.rule += verificador.rule + ",";
        verificadorList.add(verificador);
    }
}

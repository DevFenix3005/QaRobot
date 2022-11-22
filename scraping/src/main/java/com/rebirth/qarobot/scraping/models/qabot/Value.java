package com.rebirth.qarobot.scraping.models.qabot;

import lombok.Data;
import com.rebirth.qarobot.scraping.enums.MyLogicSimbols;

@Data
public class Value {

    private static final String TEXTO_INTERNO = " texto interno ";
    private static final String ATRIBUTO = " atributo: \"";

    private final String raw;
    private final String tagName;
    private final String label;
    private final String attribute;
    private final MyLogicSimbols logic;

    private final boolean attr;
    private final boolean text;

    private String content;
    private boolean interpolation;
    private String key;
    private String actualValue;


    public static Value create(String raw, String tagName, String label, String attribute, MyLogicSimbols logic, String content) {
        String attrProcess = attribute != null ? attribute.substring(1) : "Sin atributo";
        String contentProcess = content != null ? content : "Sin valor de comparacion";
        return new Value(raw, tagName, label, attrProcess, logic, contentProcess);
    }

    private Value(String raw, String tagName, String label, String attribute, MyLogicSimbols logic, String content) {
        this.raw = raw;
        this.tagName = tagName;
        this.label = label;
        this.attribute = attribute;
        this.logic = logic;
        this.content = content;
        this.attr = label.equals("attr");
        this.text = label.equals("text");
    }

    public boolean initEvaluation() {
        return this.logic.runLogic(actualValue, content);
    }


    public String descripcion(boolean negativa) {

        String messagePrefix = "El " + (attr ? ATRIBUTO + attribute + "\"" : TEXTO_INTERNO);
        String nextPartOfMessage = (negativa ? " NO" : "");

        switch (logic) {
            case EQ:
                return messagePrefix + nextPartOfMessage + " ES IGUAL A " + content;
            case GE:
                return messagePrefix + nextPartOfMessage + " ES MAYOR O IGUAL " + content;
            case GT:
                return messagePrefix + nextPartOfMessage + " ES MAYOR QUE " + content;
            case LE:
                return messagePrefix + nextPartOfMessage + " ES MENOR O IGUAL " + content;
            case LT:
                return messagePrefix + nextPartOfMessage + " ES MENOR QUE " + content;
            case CONTAIN:
                return messagePrefix + nextPartOfMessage + " CONTIENE " + content;
            case NOEMPTY:
                return messagePrefix + (negativa ? "" : " NO") + " ESTA VACIO";
            default:
                return "UNK";
        }
    }


}

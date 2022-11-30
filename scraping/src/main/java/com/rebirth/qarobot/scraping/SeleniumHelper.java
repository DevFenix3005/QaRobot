package com.rebirth.qarobot.scraping;

import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionTypeWithSelectorAndTimeOut;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionTypeWithSelectorLambdaAndTimeOut;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ChooseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.SelectorType;
import com.rebirth.qarobot.scraping.models.qabot.Value;
import com.rebirth.qarobot.scraping.utils.InterpolationResult;
import org.openqa.selenium.WebElement;
import com.rebirth.qarobot.commons.models.dtos.Verificador;
import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;
import com.rebirth.qarobot.commons.utils.PuaseExecutionFromStopAction;
import com.rebirth.qarobot.commons.utils.SendInfo2View;
import com.rebirth.qarobot.commons.utils.SendQaContext2View;
import com.rebirth.qarobot.commons.utils.ShowInDialog;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface SeleniumHelper {

    void addValue2Contexto(String key, Object value);

    String getValueFormContext(String key);

    Map<String, Object> getContextMap();

    void addVerificacion2Context(Verificador verificador);

    boolean verificacionesOk();

    List<Verificador> getVerificadores();

    void cleanContexto();

    void goToUrl(String url);

    void delay(long time);

    InterpolationResult getInterpolationOfValueIfExistsOrGetRawValue(String value);

    Value processValue(String inputValue, String tagname);

    Value processValue(String input, WebElement webElement);

    void closeDriver();

    void switchTabs(int tabNumber);

    java.awt.Point seleniumPoint2AwtPoint();

    void setSendInfo2View(SendInfo2View sendInfo2View);

    void setShowInDialog(ShowInDialog showInDialog);

    void sendAction2View(BaseActionType currentAction, Color color, Throwable throwable);

    void displayDialog(TitleIconAndMsgPojo titleIconAndMsgPojo);

    String createHtml(String body);

    void hideDialog();

    //BaseActionTypeWithSelectorAndTimeOut
    default WebElement getWebElement(BaseActionTypeWithSelectorAndTimeOut baseActionTypeWithSelectorAndTimeOut) {
        return getWebElement(baseActionTypeWithSelectorAndTimeOut.getId(), baseActionTypeWithSelectorAndTimeOut.getSelector());
    }

    default WebElement getWebElement(BaseActionTypeWithSelectorLambdaAndTimeOut baseActionTypeWithSelectorAndTimeOut) {
        return getWebElement(baseActionTypeWithSelectorAndTimeOut.getId(), baseActionTypeWithSelectorAndTimeOut.getSelector());
    }

    WebElement getWebElement(String id, List<SelectorType> selector);

    default List<WebElement> getWebElements(BaseActionTypeWithSelectorAndTimeOut baseActionTypeWithSelectorAndTimeOut) {
        return getWebElements(baseActionTypeWithSelectorAndTimeOut.getId(), baseActionTypeWithSelectorAndTimeOut.getSelector());
    }

    default List<WebElement> getWebElements(BaseActionTypeWithSelectorLambdaAndTimeOut baseActionTypeWithSelectorAndTimeOut) {
        return getWebElements(baseActionTypeWithSelectorAndTimeOut.getId(), baseActionTypeWithSelectorAndTimeOut.getSelector());
    }

    List<WebElement> getWebElements(String id, List<SelectorType> selector);


    default String getValueFromWebElement(BaseActionTypeWithSelectorAndTimeOut baseActionTypeWithSelectorAndTimeOut) {
        return getValueFromWebElement(baseActionTypeWithSelectorAndTimeOut.getId(), baseActionTypeWithSelectorAndTimeOut.getSelector());
    }

    default String getValueFromWebElement(BaseActionTypeWithSelectorLambdaAndTimeOut baseActionTypeWithSelectorAndTimeOut) {
        return getValueFromWebElement(baseActionTypeWithSelectorAndTimeOut.getId(), baseActionTypeWithSelectorAndTimeOut.getSelector());
    }

    String getValueFromWebElement(String id, List<SelectorType> selector);

    void setValueToVadiinsUglyDropdown(ChooseActionType chooseActionType);

    default String getAttributesFromElement(BaseActionTypeWithSelectorAndTimeOut baseActionTypeWithSelectorAndTimeOut, String attribute) {
        return getAttributesFromElement(baseActionTypeWithSelectorAndTimeOut.getId(), baseActionTypeWithSelectorAndTimeOut.getSelector(), attribute);
    }

    default String getAttributesFromElement(BaseActionTypeWithSelectorLambdaAndTimeOut baseActionTypeWithSelectorAndTimeOut, String attribute) {
        return getAttributesFromElement(baseActionTypeWithSelectorAndTimeOut.getId(), baseActionTypeWithSelectorAndTimeOut.getSelector(), attribute);
    }

    String getAttributesFromElement(String id, List<SelectorType> selector, String attribute);

    void setInitialTime(long iniTime);

    void actionLog(BaseActionType action);

    BaseActionType getCurrentAction();

    Future<?> submit2Executor(Runnable callable);

    void resumenActionExecution();

    void puaseActionExecution();

    void shutdownExecutor();

    void setPuaseExecutionFromStopAction(PuaseExecutionFromStopAction puaseExecutionFromStopAction);

    void pauseFromAction();

    void setSendQaContext2View(SendQaContext2View qaContext2View);

    void sendQaContet2View();
}

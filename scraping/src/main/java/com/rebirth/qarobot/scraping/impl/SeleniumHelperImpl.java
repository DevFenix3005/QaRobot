package com.rebirth.qarobot.scraping.impl;

import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.di.enums.PatternEnum;
import com.rebirth.qarobot.commons.exceptions.NoVar2InterpolationFoundInContextEx;
import com.rebirth.qarobot.commons.exceptions.NotFoundWebElement;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.models.dtos.FailureEvidence;
import com.rebirth.qarobot.commons.models.dtos.QaRobotContext;
import com.rebirth.qarobot.commons.models.dtos.Verificador;
import com.rebirth.qarobot.commons.models.dtos.dialogs.MyOwnIcos;
import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ChooseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ItemType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.KindOfBy;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ListType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.MessageActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.SelectorType;
import com.rebirth.qarobot.commons.models.dtos.tables.ActionColorAndExIfExits;
import com.rebirth.qarobot.commons.utils.Constantes;
import com.rebirth.qarobot.commons.utils.PuaseExecutionFromStopAction;
import com.rebirth.qarobot.commons.utils.SendInfo2View;
import com.rebirth.qarobot.commons.utils.SendQaContext2View;
import com.rebirth.qarobot.commons.utils.ShowInDialog;
import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.enums.MyLogicSimbols;
import com.rebirth.qarobot.scraping.models.qabot.Value;
import com.rebirth.qarobot.scraping.utils.InterpolationResult;
import com.rebirth.qarobot.scraping.utils.FailureEvidenceCollector;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Slf4j
@ChildComponent()
public final class SeleniumHelperImpl implements SeleniumHelper {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Configuracion configuracion;
    private final QaRobotContext qaRobotContext;
    private final Random random;
    private final Pattern attributePattern;
    private final Pattern interpolationPattern;
    private final Pattern verifyElementPattern;

    private SendInfo2View sendInfo2View;
    private ShowInDialog showInDialog;
    private PuaseExecutionFromStopAction puaseExecutionFromStopAction;

    private static final TimeUnit TIME_METRIC = TimeUnit.MILLISECONDS;
    private SendQaContext2View qaContext2View;
    private String lastResolvedSelector;
    private FailureEvidenceCollector failureEvidenceCollector;

    @Inject
    public SeleniumHelperImpl(WebDriver webDriver,
                              WebDriverWait webDriverWait,
                              Configuracion configuracion,
                              QaRobotContext qaRobotContext,
                              Random random,
                              Map<PatternEnum, Pattern> patternEnumPatternMap) {
        this.driver = webDriver;
        this.wait = webDriverWait;
        this.configuracion = configuracion;
        this.qaRobotContext = qaRobotContext;
        this.random = random;
        this.attributePattern = patternEnumPatternMap.get(PatternEnum.ATTRIBUTE_PATTERN);
        this.interpolationPattern = patternEnumPatternMap.get(PatternEnum.INTERPOLATION_PATTERN);
        this.verifyElementPattern = patternEnumPatternMap.get(PatternEnum.VERIFYELEMENT_PATTERN);

        if (!Boolean.getBoolean("qarobot.headless") && !GraphicsEnvironment.isHeadless()) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            double width = screenSize.getWidth();
            Point point = new Point((int) width, 0);
            WebDriver.Window window = this.driver.manage().window();
            window.setPosition(point);
            window.maximize();
        }
    }

    @Override
    public void addValue2Contexto(String key, Object value) {
        this.qaRobotContext.addValue(key, value);
    }

    @Override
    public String getValueFormContext(String key) {
        return this.qaRobotContext.getValue(key).toString();
    }

    @Override
    public Map<String, Object> getContextMap() {
        return this.qaRobotContext.getMapContainer();
    }

    @Override
    public void addVerificacion2Context(Verificador verificador) {
        this.qaRobotContext.addVerificador(verificador);
        if (!verificador.isOk() && !verificador.isSkip()) {
            captureEvidence(getCurrentAction(), null, verificador);
        }
    }

    @Override
    public void initializeFailureEvidence(Path reportDirectory) {
        failureEvidenceCollector = new FailureEvidenceCollector(driver, reportDirectory);
    }

    @Override
    public void captureFailure(BaseActionType action, Throwable failure) {
        captureEvidence(action, failure, null);
    }

    @Override
    public List<FailureEvidence> getFailureEvidence() {
        return failureEvidenceCollector == null ? List.of() : failureEvidenceCollector.getEvidence();
    }

    private void captureEvidence(BaseActionType action, Throwable failure, Verificador verification) {
        if (failureEvidenceCollector == null) return;
        try {
            failureEvidenceCollector.capture(action, lastResolvedSelector, failure, verification);
        } catch (RuntimeException captureFailure) {
            log.warn("No se pudo completar la evidencia del fallo", captureFailure);
        }
    }

    @Override
    public boolean verificacionesOk() {
        return this.qaRobotContext.allValidacionesOK();
    }

    public List<Verificador> getVerificadores() {
        return qaRobotContext.getVerificadores();
    }

    @Override
    public void cleanContexto() {
        this.qaRobotContext.reset();
    }

    public void goToUrl(String url) {
        this.driver.get(url);
    }

    @Override
    public WebElement getWebElement(String id, List<SelectorType> selectors) {
        return getWebElement(id, selectors, ElementReadiness.PRESENT, null);
    }

    @Override
    public WebElement getWebElement(String id, List<SelectorType> selectors,
                                    ElementReadiness readiness, BigInteger waitTimeout) {
        return waitForElement(id, selectors, waitTimeout, readiness.name(), element -> isReady(element, readiness));
    }

    private WebElement waitForElement(String id, List<SelectorType> selectors, BigInteger waitTimeout,
                                      String condition, Predicate<WebElement> ready) {
        if (selectors.isEmpty()) {
            throw new NotFoundWebElement(id, "No selectors configured for action " + id, selectors);
        }
        // Check all alternatives in each poll so a missing first selector does not consume the whole deadline.
        try {
            return actionWait(waitTimeout).until(currentDriver -> {
                for (SelectorType selector : selectors) {
                    try {
                        WebElement element = currentDriver.findElement(processBy(selector));
                        lastResolvedSelector = describeSelector(selector);
                        if (ready.test(element)) return element;
                    } catch (NoSuchElementException | StaleElementReferenceException transientFailure) {
                        // The DOM may change between locating the element and checking its state.
                    }
                }
                return null;
            });
        } catch (TimeoutException timeout) {
            throw waitFailure(id, selectors, condition, waitTimeout, timeout);
        }
    }

    private boolean isReady(WebElement element, ElementReadiness readiness) {
        return switch (readiness) {
            case PRESENT -> true;
            case VISIBLE -> element.isDisplayed();
            case CLICKABLE -> element.isDisplayed() && element.isEnabled();
            case EDITABLE -> element.isDisplayed() && element.isEnabled()
                    && !"true".equalsIgnoreCase(element.getDomAttribute("readonly"));
        };
    }

    private WebDriverWait actionWait(BigInteger waitTimeout) {
        if (waitTimeout == null) return wait;
        long milliseconds = waitTimeout.longValueExact();
        if (milliseconds < 0) throw new IllegalArgumentException("waitTimeout must be non-negative");
        return new WebDriverWait(driver, Duration.ofMillis(milliseconds), Duration.ofMillis(100));
    }

    private NotFoundWebElement waitFailure(String id, List<SelectorType> selectors, String condition,
                                           BigInteger waitTimeout, TimeoutException timeout) {
        long milliseconds = waitTimeout == null ? configuracion.timeout() : waitTimeout.longValueExact();
        String attempted = selectors.stream().map(this::describeSelector).collect(java.util.stream.Collectors.joining(", "));
        NotFoundWebElement failure = new NotFoundWebElement(id,
                "Action " + id + " timed out after " + milliseconds + " ms waiting for " + condition
                        + "; selectors: " + attempted, selectors);
        failure.initCause(timeout);
        return failure;
    }

    private String describeSelector(SelectorType selector) {
        return selector.getBy() + ": " + selector.getValue();
    }

    @Override
    public List<WebElement> getWebElements(String id, List<SelectorType> selectors) {
        return getWebElements(id, selectors, null);
    }

    @Override
    public List<WebElement> getWebElements(String id, List<SelectorType> selectors, BigInteger waitTimeout) {
        if (selectors.isEmpty()) {
            throw new NotFoundWebElement(id, "No selectors configured for action " + id, selectors);
        }
        try {
            return actionWait(waitTimeout).until(currentDriver -> {
                for (SelectorType selector : selectors) {
                    List<WebElement> elements = currentDriver.findElements(processBy(selector));
                    if (!elements.isEmpty()) {
                        lastResolvedSelector = describeSelector(selector);
                        return elements;
                    }
                }
                return null;
            });
        } catch (TimeoutException timeout) {
            throw waitFailure(id, selectors, "PRESENT", waitTimeout, timeout);
        }
    }


    private By processBy(SelectorType selector) {
        KindOfBy by = selector.getBy();
        String path = selector.getValue();
        return switch (by) {
            case CSS -> By.cssSelector(path);
            case ID -> By.id(path);
            default -> By.xpath(path);
        };
    }


    @Override
    public String getValueFromWebElement(String id, List<SelectorType> selector) {
        return getValueFromWebElement(this.getWebElement(id, selector));
    }

    @Override
    public String getValueFromWebElement(WebElement webElement) {
        String value = getShowedValue(webElement);
        value = new String(value.getBytes(StandardCharsets.ISO_8859_1));
        return normilizeText(value);
    }

    private String getShowedValue(WebElement webElement) {
        String tagName = webElement.getTagName();
        switch (tagName) {
            case "input", "textarea":
                return webElement.getAttribute(Constantes.VALUE);
            case "select":
                Select select = new Select(webElement);
                WebElement option = select.getFirstSelectedOption();
                return option.getText();
            default:
                return webElement.getText();
        }
    }


    @Override
    public void setValueToVadiinsUglyDropdown(ChooseActionType chooseActionType) {
        String id = chooseActionType.getId();
        List<SelectorType> selector = chooseActionType.getSelector();
        String value = chooseActionType.getValue();
        ListType listaOpciones = chooseActionType.getList();

        String realValue = getRealValue(value, listaOpciones);

        WebElement webElement = waitForElement(id, selector, chooseActionType.getWaitTimeout(),
                "CLICKABLE with option " + realValue, candidate -> {
                    if (!isReady(candidate, ElementReadiness.CLICKABLE)) return false;
                    if (!"select".equalsIgnoreCase(candidate.getTagName())) return true;
                    return new Select(candidate).getOptions().stream()
                            .anyMatch(option -> realValue.equals(option.getText()) && option.isEnabled());
                });

        NotFoundWebElement notFoundWebElement = new NotFoundWebElement(id, "No se encontro la opcion dentro del select la opcion " + realValue, selector);

        String tagName = webElement.getTagName().toLowerCase(Locale.ROOT);
        if (tagName.equals("select")) {
            try {
                Select select = new Select(webElement);
                select.selectByVisibleText(realValue);
            } catch (NoSuchElementException noSuchElementException) {
                throw notFoundWebElement;
            }
        } else {
            vadiinProcessToselectValue(webElement, realValue, notFoundWebElement);
        }
    }

    private void vadiinProcessToselectValue(WebElement webElement, String realValue, NotFoundWebElement notFoundWebElement) {
        webElement.click();
        webElement.sendKeys(Keys.ARROW_DOWN);
        webElement.sendKeys(Keys.ARROW_DOWN);

        for (int i = 0; i < 30; i++) {
            webElement.sendKeys(Keys.ARROW_UP);
        }

        int j = 0;
        while (true) {
            String currentValue = webElement.getAttribute(Constantes.VALUE);
            if (currentValue.equals(realValue)) {
                webElement.sendKeys(Keys.ENTER);
                break;
            } else {
                webElement.sendKeys(Keys.ARROW_DOWN);
                delay(222);
                if ((j++) > 100) {
                    throw notFoundWebElement;
                }
            }
        }
    }

    private String getRealValue(String value, ListType listaOpciones) {
        String realValue;
        if (listaOpciones != null && value.equals(Constantes.RANDOM_LIST)) {
            List<String> chooseList = listaOpciones.getItem().stream().map(ItemType::getValue).toList();
            int randomValue = random.nextInt(chooseList.size());
            realValue = chooseList.get(randomValue);
        } else {
            InterpolationResult interpolation = this.getInterpolationOfValueIfExistsOrGetRawValue(value);
            realValue = interpolation.getValue();
        }
        return realValue;
    }

    @Override
    public String getAttributesFromElement(String id, List<SelectorType> selectorTypes, String attribute) {
        WebElement element = this.getWebElement(id, selectorTypes);
        return element.getAttribute(attribute);
    }

    @Override
    public void setInitialTime(long iniTime) {
        this.qaRobotContext.getCurrentTime().set(iniTime);
    }

    @Override
    public void delay(long time) {
        try {
            TIME_METRIC.sleep(time);
        } catch (InterruptedException e) {
            log.error("Delay error", e);
            Thread.currentThread().interrupt();
        }
    }


    private String normilizeText(String input) {
        input = input.trim();
        input = Normalizer.normalize(input, Normalizer.Form.NFD);
        input = input.replaceAll("[^\\p{ASCII}]", "");
        input = Pattern.compile("\u00d1", Pattern.CANON_EQ).matcher(input).replaceAll("N");
        input = Pattern.compile("\u00f1", Pattern.CANON_EQ).matcher(input).replaceAll("n");
        return input;
    }


    public Value processValue(String raw, WebElement element) {
        String tagName = element.getTagName();
        Value valueObj = raw2ValueObject(raw, tagName);
        String actualValue = "";
        String label = valueObj.getLabel();
        if ("text".equals(label)) {
            actualValue = getShowedValue(element);
        } else if ("attr".equals(label)) {
            actualValue = element.getAttribute(valueObj.getAttribute());
        }
        valueObj.setActualValue(actualValue);
        return valueObj;
    }


    public Value processValue(String raw, String tagname) {
        return raw2ValueObject(raw, tagname);
    }

    private Value raw2ValueObject(String raw, String tagname) {
        Value valueObj;
        Matcher matcherVerifyElement = verifyElementPattern.matcher(raw);

        if (matcherVerifyElement.matches()) {
            String label = matcherVerifyElement.group("label");
            String attribute = matcherVerifyElement.group("attribute");
            //String eqq = matcherVerifyElement.group("eqq");
            String logic = matcherVerifyElement.group("logic");
            //String dot = matcherVerifyElement.group("dot");
            String content = matcherVerifyElement.group("content");
            MyLogicSimbols realLogic = MyLogicSimbols.valueOf(logic.toUpperCase(Locale.ROOT));
            valueObj = Value.create(raw, tagname, label, attribute, realLogic, content);
        } else {
            valueObj = Value.create(raw, tagname, "text", null, MyLogicSimbols.EQ, raw);
        }
        InterpolationResult interpolationValue = getInterpolationOfValueIfExistsOrGetRawValue(valueObj.getContent());
        if (interpolationValue.isInterpolation()) {
            valueObj.setInterpolation(true);
            valueObj.setKey(interpolationValue.getKey());
            valueObj.setContent(interpolationValue.getValue());
        }
        return valueObj;
    }

    public InterpolationResult getInterpolationOfValueIfExistsOrGetRawValue(String value) {
        Matcher matcherInterpolation = interpolationPattern.matcher(value);
        if (matcherInterpolation.matches()) {
            String mapKey = matcherInterpolation.group(Constantes.VALUE);
            Object rawValue = this.qaRobotContext.getValue(mapKey);
            if (rawValue == null) {
                this.qaRobotContext.addValue(mapKey, "¡¡¡¡NO EXISTE!!!!");
                throw new NoVar2InterpolationFoundInContextEx(mapKey);
            } else {
                return InterpolationResult.create(mapKey, rawValue.toString(), true);
            }
        } else return InterpolationResult.create("UNK", value, true);

    }

    @Override
    public void closeDriver() {
        this.driver.quit();
    }

    @Override
    public void switchTabs(int tabNumber) {
        List<String> tabs = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo()
                .window(tabs.get(0))
                .close();

        driver.switchTo()
                .window(tabs.get(tabNumber));
    }

    public java.awt.Point seleniumPoint2AwtPoint() {
        Point point = this.driver.manage().window().getPosition();
        java.awt.Point pointAwt = new java.awt.Point();
        pointAwt.x = point.x;
        pointAwt.y = point.y;
        return pointAwt;
    }

    @Override
    public void setSendInfo2View(SendInfo2View sendInfo2View) {
        this.sendInfo2View = sendInfo2View;
    }

    @Override
    public void setShowInDialog(ShowInDialog showInDialog) {
        this.showInDialog = showInDialog;
    }

    @Override
    public void sendAction2View(BaseActionType currentAction, Color color, Throwable throwable) {
        if (sendInfo2View != null) {
            ActionColorAndExIfExits actionColorAndExIfExits = ActionColorAndExIfExits.create(currentAction, color);
            if (throwable != null) actionColorAndExIfExits.setThrowable(throwable);
            sendInfo2View.send(actionColorAndExIfExits);
        }
    }

    @Override
    public void displayDialog(TitleIconAndMsgPojo titleIconAndMsgPojo) {
        if (this.showInDialog != null) {
            this.showInDialog.run(titleIconAndMsgPojo);
        }
    }

    @Override
    public void hideDialog() {
        if (this.showInDialog != null) {
            this.showInDialog.hideDialog();
        }
    }

    public String createHtml(String body) {
        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<body>" +
                body +
                "</body>" +
                "</html>";
    }

    @Override
    public void actionLog(BaseActionType action) {
        this.qaRobotContext.getCurrentAction().set(action);
        this.lastResolvedSelector = null;
        boolean iterationChild = action.isIterationChild();
        String id = action.getId();
        String desc = action.getDesc();
        Long order = action.getOrder();
        String type = action.getClass().getSimpleName();
        boolean skip = action.isSkip();
        String template = "Accion[{}] ID:{} Orden:{} Descripcion:{}";

        if (iterationChild) {
            template = "Corriendo en iteracion" + "[" + template + "]";
        } else {
            if (skip) {
                template = "OMITIDA!!!![ " + template + " ]";
            }
            this.sendAction2View(action, Color.CYAN, null);
            if (!(action instanceof MessageActionType) && !skip) {
                sendMessage2Dialog(action);
            }
        }
        log.info(template, type, id, order, desc);
    }

    private void sendMessage2Dialog(BaseActionType baseActionDto) {
        long elapsedTime = System.currentTimeMillis() - this.qaRobotContext.getCurrentTime().get();
        int seconds = (int) (elapsedTime / 1000) % 60;
        int minutes = (int) ((elapsedTime / (1000 * 60)) % 60);
        int hours = (int) ((elapsedTime / (1000 * 60 * 60)) % 24);

        String elapsedTimeString;
        if (hours > 0) {
            elapsedTimeString = String.format("%02d mins, %02d seg.", minutes, seconds);
        } else {
            elapsedTimeString = String.format("%02d hr, %02d mins, %02d seg.", hours, minutes, seconds);
        }

        String fechaDeEjecucion = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String htmlTemplate = "<ul>" +
                ilElement("Tiempo Transcurrido:" + elapsedTimeString) +
                ilElement("ID:" + baseActionDto.getId()) +
                ilElement("Descripcion:" + baseActionDto.getDesc()) +
                ilElement("Hora:" + fechaDeEjecucion) +
                ilElement("Tipo de accion:" + baseActionDto.getClass().getSimpleName()) +
                "</ul>";

        TitleIconAndMsgPojo titleIconAndMsgPojo = TitleIconAndMsgPojo.create("Mensaje", htmlTemplate, MyOwnIcos.ROBOT_MDPI);
        titleIconAndMsgPojo.setDimension(new Dimension(600, 400));
        titleIconAndMsgPojo.setPoint(seleniumPoint2AwtPoint());
        titleIconAndMsgPojo.setClose(false);
        titleIconAndMsgPojo.setRunInThread(false);
        titleIconAndMsgPojo.setCloseAfter(0);
        displayDialog(titleIconAndMsgPojo);
        delay(2500);
        hideDialog();
    }

    private String ilElement(String content) {
        return "<li>" + content + "</li>";
    }

    @Override
    public BaseActionType getCurrentAction() {
        return this.qaRobotContext.getCurrentAction().get();
    }

    @Override
    public Future<?> submit2Executor(Runnable runnable) {
        return this.qaRobotContext.submitCallableInExecutor(runnable);
    }

    @Override
    public void resumenActionExecution() {
        this.qaRobotContext.resumenExecutor();
    }

    @Override
    public void puaseActionExecution() {
        this.qaRobotContext.pauseExecutor();
    }

    @Override
    public void shutdownExecutor() {
        this.qaRobotContext.shuwdownExecutor();
    }

    @Override
    public void setPuaseExecutionFromStopAction(PuaseExecutionFromStopAction puaseExecutionFromStopAction) {
        this.puaseExecutionFromStopAction = puaseExecutionFromStopAction;
    }

    @Override
    public void pauseFromAction() {
        if (this.puaseExecutionFromStopAction != null) {
            this.puaseExecutionFromStopAction.pause();
        }
    }

    @Override
    public void setSendQaContext2View(SendQaContext2View qaContext2View) {
        this.qaContext2View = qaContext2View;
    }

    @Override
    public void sendQaContet2View() {
        if (this.qaRobotContext != null) {
            this.qaContext2View.send(this.qaRobotContext);
        }
    }

}

package com.rebirth.qarobot.scraping.impl;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import com.rebirth.qarobot.scraping.utils.ActionRunner;
import com.rebirth.qarobot.scraping.utils.AssetsFilterFile;
import com.rebirth.qarobot.scraping.utils.predicates.OkPredicate;
import com.rebirth.qarobot.scraping.utils.predicates.SkipPredicate;
import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.exceptions.NoVar2InterpolationFoundInContextEx;
import com.rebirth.qarobot.commons.exceptions.NotFoundQaRobotConfigurationEx;
import com.rebirth.qarobot.commons.exceptions.NotFoundWebElement;
import com.rebirth.qarobot.commons.exceptions.StopActionException;
import com.rebirth.qarobot.commons.models.dtos.QarobotWrapper;
import com.rebirth.qarobot.commons.models.dtos.Verificador;
import com.rebirth.qarobot.commons.models.dtos.dialogs.MyOwnIcos;
import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ConfigurationType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.SelectorType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.SetType;
import com.rebirth.qarobot.commons.models.dtos.screws.SetValue;
import com.rebirth.qarobot.commons.utils.PuaseExecutionFromStopAction;
import com.rebirth.qarobot.commons.utils.SendInfo2View;
import com.rebirth.qarobot.commons.utils.SendQaContext2View;
import com.rebirth.qarobot.commons.utils.ShowInDialog;
import com.rebirth.qarobot.scraping.QaRobotXml;
import com.rebirth.qarobot.scraping.SeleniumHelper;

import javax.inject.Inject;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Stream;

@ChildComponent()
@Data
@EqualsAndHashCode(callSuper = false)
@Log4j2
public final class QaRobotXmlImpl implements QaRobotXml {

    private final SeleniumHelper seleniumHelper;
    private final Map<Class<? extends BaseActionType>, Action<? extends BaseActionType>> actionMap;
    private final Template template;
    private QarobotWrapper mainQaRobot;

    @Inject
    public QaRobotXmlImpl(
            SeleniumHelper seleniumHelper,
            Map<Class<? extends BaseActionType>, Action<? extends BaseActionType>> actionMap,
            Template template
    ) {
        this.seleniumHelper = seleniumHelper;
        this.actionMap = actionMap;
        this.template = template;
    }

    public void setQaRobot(QarobotWrapper mainQaRobot) {
        this.mainQaRobot = mainQaRobot;
    }

    public boolean flux() {
        Throwable exception = null;
        try {
            this.seleniumHelper.setInitialTime(System.currentTimeMillis());
            this.robotConfigurationProcess();
            this.startInvokeActions();
        } catch (NotFoundQaRobotConfigurationEx notFoundQaRobotConfigurationEx) {
            exception = notFoundQaRobotConfigurationEx;
            log.error("QaRobotXml::flux->NotFoundQaRobotConfigurationEx", notFoundQaRobotConfigurationEx);
        } catch (ExecutionException executionException) {
            Throwable throwable = executionException.getCause();
            if (throwable instanceof NotFoundWebElement notFoundWebElement) {
                exception = notFoundWebElement;
                for (SelectorType selector : notFoundWebElement.getSelectors()) {
                    log.info("Se trato de buscar por: {}", selector.toString());
                }
            } else if (throwable instanceof NoVar2InterpolationFoundInContextEx noVar2InterpolationFoundInContextEx) {
                exception = noVar2InterpolationFoundInContextEx;
                log.error("QaRobotXml::flux->NoVar2InterpolationFoundInContextEx", noVar2InterpolationFoundInContextEx);
            } else if (throwable instanceof StopActionException stopActionException) {
                List<BaseActionType> actions = mainQaRobot.getActions();
                int stopIndex = actions.indexOf(stopActionException.getStopActionType());
                for (int i = stopIndex + 1; i < actions.size(); i++) {
                    BaseActionType action = actions.get(i);
                    seleniumHelper.sendAction2View(action, Color.YELLOW, null);
                }
            } else {
                exception = throwable;
            }
        } finally {
            if (exception != null) {

                String exMessage = exception.getMessage();
                exMessage = Objects.isNull(exMessage) ? "Sin mensaje" : exMessage;

                this.seleniumHelper.addValue2Contexto("ERROR", exMessage);
                this.seleniumHelper.sendAction2View(seleniumHelper.getCurrentAction(), Color.RED, exception);
                this.seleniumHelper.displayDialog(TitleIconAndMsgPojo.create("Error", "<p>" + exMessage + "</p>", MyOwnIcos.ERROR_MDPI));
            }
            debugMapContainer();
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 Writer out = new OutputStreamWriter(baos, StandardCharsets.ISO_8859_1)
            ) {
                File indexFile = generateDashbord(out, exception);
                Files.asByteSink(indexFile).write(baos.toByteArray());
                String userHome = StandardSystemProperty.USER_DIR.value();
                File dashboardFolder = new File(userHome + File.separator + "dashboardtemplate");
                File[] assets = dashboardFolder.listFiles(new AssetsFilterFile());
                if (Objects.nonNull(assets)) {
                    for (File asset : assets) {
                        copyAssets(asset);
                    }
                }
                Desktop.getDesktop().browse(indexFile.toURI());
            } catch (TemplateException notFoundQaRobotConfigurationEx) {
                exception = notFoundQaRobotConfigurationEx;
                log.error("QaRobotXml::flux->TemplateException", notFoundQaRobotConfigurationEx);
            } catch (IOException notFoundQaRobotConfigurationEx) {
                exception = notFoundQaRobotConfigurationEx;
                log.error("QaRobotXml::flux->IOException", notFoundQaRobotConfigurationEx);
            }
        }
        return exception == null;
    }

    @Override
    public void delegateSenders2SeleniumHelper(SendInfo2View sendInfo2View, ShowInDialog showInDialog,
                                               PuaseExecutionFromStopAction puaseExecutionFromStopAction,
                                               SendQaContext2View qaContext2View) {
        this.seleniumHelper.setSendInfo2View(sendInfo2View);
        this.seleniumHelper.setShowInDialog(showInDialog);
        this.seleniumHelper.setPuaseExecutionFromStopAction(puaseExecutionFromStopAction);
        this.seleniumHelper.setSendQaContext2View(qaContext2View);
    }


    private void copyAssets(File asset) {
        try {
            Files.copy(asset, new File(mainQaRobot.getDashboardExitFile(), asset.getName()));
        } catch (IOException ioException) {
            log.error("Error en la copia del asset " + asset.getName(), ioException);
        }
    }


    private void debugMapContainer() {
        List<SetValue> setValueList = this.seleniumHelper
                .getContextMap()
                .entrySet()
                .stream()
                .map(entry -> new SetValue(entry.getKey(), entry.getValue().toString()))
                .toList();

        String mapContainerAsciiTable = AsciiTable.getTable(setValueList, Lists.newArrayList(
                new Column().header("Llave").with(SetValue::getKey),
                new Column().header("Valor").with(SetValue::getValue)
        ));
        log.info("\n" + mapContainerAsciiTable + "\n");
    }

    private void startInvokeActions() throws ExecutionException {

        List<BaseActionType> actions = mainQaRobot.getActions();
        List<Runnable> runnableList = Lists.newArrayList();
        for (BaseActionType action : actions) {
            ActionRunner actionRunner = new ActionRunner(action, actionMap);
            runnableList.add(actionRunner);
        }

        try {
            for (Runnable runnable : runnableList) {
                Future<?> future = this.seleniumHelper.submit2Executor(runnable);
                try {
                    future.get();
                } catch (InterruptedException e) {
                    log.error("QaRobotXmlImpl::startInvokeActions -> InterruptedException", e);
                    Thread.currentThread().interrupt();
                } finally {
                    this.seleniumHelper.sendQaContet2View();
                }

            }
        } finally {
            this.seleniumHelper.shutdownExecutor();
        }
    }

    public void pauseExecution() {
        seleniumHelper.puaseActionExecution();
    }

    public void resumenExecution() {
        seleniumHelper.resumenActionExecution();
    }

    private File generateDashbord(Writer out, Throwable throwable) throws TemplateException, IOException {
        File file = new File(mainQaRobot.getDashboardExitFile(), "index.html");
        List<SetValue> setValuesList = this.seleniumHelper.getContextMap().entrySet().stream().map(entry -> {
            String value = entry.getValue().toString();
            String key = entry.getKey();
            return new SetValue(key, value);
        }).toList();

        List<Verificador> listaVerificaion = this.seleniumHelper.getVerificadores();

        OkPredicate okPredicate = new OkPredicate();
        SkipPredicate skipPredicate = new SkipPredicate();

        Predicate<Verificador> noSkipPredicate = skipPredicate.negate();
        Predicate<Verificador> okAndNotSkipPredicate = okPredicate.and(noSkipPredicate);
        Predicate<Verificador> errorAndNotSkipPredicate = okPredicate.negate().and(noSkipPredicate);

        String title = this.mainQaRobot.getFileName();
        if (throwable != null) {
            title = "[¡¡Finalizo con Error!!]" + title;
        }

        List<BaseActionType> actionTypes = this.mainQaRobot.getActions();

        Map<String, Object> root = new HashMap<>();
        root.put("myConfigvalues", setValuesList);
        root.put("actionsdto", actionTypes);
        root.put("verificadors", listaVerificaion);
        root.put("counterAcciones", actionTypes.size());
        root.put("counterPruebasOk", listaVerificaion.stream().filter(okAndNotSkipPredicate).count());
        root.put("counterPruebasErr", listaVerificaion.stream().filter(errorAndNotSkipPredicate).count());
        root.put("counterPruebasSkip", listaVerificaion.stream().filter(skipPredicate).count());
        root.put("execxml", Files.asCharSource(this.mainQaRobot.getXmlTempFile(), StandardCharsets.UTF_8).read());
        root.put("dashboartitle", title);
        Environment env = template.createProcessingEnvironment(root, out);
        env.setOutputEncoding(StandardCharsets.ISO_8859_1.toString());
        env.process();
        return file;
    }

    private void robotConfigurationProcess() throws NotFoundQaRobotConfigurationEx {
        ConfigurationType robotConfiguration = mainQaRobot.getConfiguration();
        if (robotConfiguration == null) {
            throw new NotFoundQaRobotConfigurationEx(
                    "El Robot no tiene inlcuida una configuracion",
                    new NullPointerException()
            );
        } else {
            String propertiesPath = robotConfiguration.getProperties();
            if (propertiesPath != null) {
                File propertiesFile = new File(propertiesPath);
                try (InputStream is = new FileInputStream(propertiesFile)) {
                    Properties prop = new Properties();
                    prop.load(is);
                    prop.forEach((key, value) -> this.seleniumHelper.addValue2Contexto(key.toString(), value.toString()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (SetType setValue : robotConfiguration.getSet()) {
                String key = setValue.getKey();
                String value = setValue.getValue();
                this.seleniumHelper.addValue2Contexto(key, this.seleniumHelper.processValue(value, "set").getContent());
            }
        }
    }

    @Override
    public void close() {
        this.seleniumHelper.cleanContexto();
        this.seleniumHelper.closeDriver();
        this.getMainQaRobot().setDashboardExitFile(null);
    }
}

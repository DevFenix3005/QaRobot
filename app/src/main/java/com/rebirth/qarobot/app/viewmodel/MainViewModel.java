package com.rebirth.qarobot.app.viewmodel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.rebirth.qarobot.app.main.QAMaster;
import com.rebirth.qarobot.commons.exceptions.NoQaRobotXmlValid;
import com.rebirth.qarobot.commons.exceptions.NotFoundQaXmlFile;
import com.rebirth.qarobot.commons.exceptions.NotFoundWebElement;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.models.dtos.QaRobotContext;
import com.rebirth.qarobot.commons.models.dtos.dialogs.MyOwnIcos;
import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.KindOfBy;
import com.rebirth.qarobot.commons.models.dtos.qarobot.SelectorType;
import com.rebirth.qarobot.commons.models.dtos.tables.ActionColorAndExIfExits;
import com.rebirth.qarobot.commons.models.dtos.toggle.PauseOrResumeState;
import com.rebirth.qarobot.scraping.enums.Browser;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Singleton
public class MainViewModel {

    private final BehaviorSubject<List<BaseActionType>> actionDtos = BehaviorSubject.createDefault(Lists.newArrayList());

    private final BehaviorSubject<String> statusBar = BehaviorSubject.createDefault("Selecciona una prueba XML para comenzar");

    private final BehaviorSubject<Boolean> grabar = BehaviorSubject.createDefault(Boolean.FALSE);

    private final BehaviorSubject<Integer> interacion = BehaviorSubject.createDefault(1);

    private final BehaviorSubject<Browser> browser = BehaviorSubject.createDefault(Browser.CHROME);

    private final BehaviorSubject<Boolean> searchButton = BehaviorSubject.createDefault(Boolean.TRUE);

    private final BehaviorSubject<Boolean> statusInitButton = BehaviorSubject.createDefault(Boolean.FALSE);

    private final BehaviorSubject<Boolean> statusReloadButton = BehaviorSubject.createDefault(Boolean.FALSE);

    private final BehaviorSubject<Boolean> pauseOrResumenStatus = BehaviorSubject.createDefault(Boolean.FALSE);

    private final BehaviorSubject<GraphicsDevice> selectedScreen = BehaviorSubject.create();

    private final BehaviorSubject<String> xmlPath = BehaviorSubject.create();

    private final BehaviorSubject<ActionColorAndExIfExits> interaccionData2ChangeAdvanceInTable = BehaviorSubject.create();

    private final BehaviorSubject<File> selectedFile = BehaviorSubject.create();

    private final BehaviorSubject<TitleIconAndMsgPojo> sendInfo2MyDialog = BehaviorSubject.create();

    private final BehaviorSubject<Boolean> hideMyDialog = BehaviorSubject.create();

    private final BehaviorSubject<PauseOrResumeState> pauseOrResumenActionExecution = BehaviorSubject.create();

    private final BehaviorSubject<QaRobotContext> showEvalTable = BehaviorSubject.create();

    private final QAMaster qaMaster;

    private final AtomicReference<String> md5Storage = new AtomicReference<>();

    private final Configuracion configuracion;

    @Inject
    public MainViewModel(QAMaster qaMaster, Configuracion configuracion) {
        this.qaMaster = qaMaster;
        this.configuracion = configuracion;
    }

    public void chooseXmlPath(Component owner) {
        if (!Boolean.TRUE.equals(searchButton.getValue()))
            return;
        JFileChooser fileChooser = new JFileChooser(configuracion.getXmlHome());
        fileChooser.setDialogTitle("Seleccionar prueba XML");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Pruebas QaRobot (*.xml)", "xml"));
        if (fileChooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) {
            selectedFile.onNext(fileChooser.getSelectedFile());
            loadSelectedXml();
        }
    }

    public void reloadQaRobot() {
        if (!Boolean.TRUE.equals(statusReloadButton.getValue()))
            return;
        loadSelectedXml();
    }

    private void loadSelectedXml() {
        File xml = selectedFile.getValue();
        if (xml == null)
            return;
        statusBar.onNext("Cargando prueba XML…");
        statusInitButton.onNext(false);
        try {
            HashCode hash = Files.asByteSource(xml).hash(Hashing.sha256());
            md5Storage.set(hash.toString().toUpperCase(Locale.ROOT));
            loadXml2QaRobot();
            if (Boolean.TRUE.equals(statusInitButton.getValue())) {
                statusBar.onNext("Prueba lista para ejecutar");
            }
        } catch (IOException | NoQaRobotXmlValid ex) {
            finishQaWithError(ex);
        }
    }

    private void loadXml2QaRobot() {
        File xmlFile = selectedFile.getValue();
        if (Objects.nonNull(xmlFile)) {
            qaMaster.readQaRobot(xmlFile);
        }
    }

    public void startQaRobot() {
        if (!Boolean.TRUE.equals(statusInitButton.getValue()))
            return;
        pauseOrResumenActionExecution.onNext(PauseOrResumeState.NONE);
        statusInitButton.onNext(false);
        statusReloadButton.onNext(false);
        searchButton.onNext(false);
        pauseOrResumenStatus.onNext(true);
        statusBar.onNext("Iniciando prueba…");
        cleanTableColor();
        new Thread(qaMaster, "qarobot-runner").start();
    }

    public void changeComboItem(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() != ItemEvent.SELECTED)
            return;
        Browser currentBrowser = (Browser) itemEvent.getItem();
        this.browser.onNext(currentBrowser);
    }

    public void changeComboItemGraphicsDevice(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() != ItemEvent.SELECTED)
            return;
        GraphicsDevice graphicsDevice = (GraphicsDevice) itemEvent.getItem();
        this.selectedScreen.onNext(graphicsDevice);
    }

    public void cleanTableColor() {
        List<BaseActionType> currentList = actionDtos.getValue();
        if (currentList != null && !currentList.isEmpty()) {
            actionDtos.onNext(new ArrayList<>(currentList));
        }
    }

    public void restartStatusButtons() {
        boolean canRestart = Boolean.TRUE.equals(statusInitButton.getValue())
                || Boolean.TRUE.equals(pauseOrResumenStatus.getValue());
        this.searchButton.onNext(true);
        this.statusInitButton.onNext(canRestart && qaMaster.getQarobot() != null && qaMaster.getQarobot().isValidXml());
        this.statusReloadButton.onNext(selectedFile.getValue() != null);
        this.pauseOrResumenStatus.onNext(false);

    }

    public void finishQa() {
        Runnable finish = () -> {
            restartStatusButtons();
            statusBar.onNext("Prueba terminada");
            sendInfo2MyDialog.onNext(TitleIconAndMsgPojo.create("Prueba completada",
                    "<html><body><p>La ejecución terminó correctamente.</p>"
                            + "<p>Consulta el reporte HTML para revisar los resultados.</p></body></html>",
                    MyOwnIcos.INFO_MDPI));
        };
        if (SwingUtilities.isEventDispatchThread()) {
            finish.run();
        } else {
            SwingUtilities.invokeLater(finish);
        }
    }

    public void finishQaWithError(Throwable ex) {
        restartStatusButtons();
        statusBar.onNext("No se pudo completar la prueba");
        log.error("Error en la aplicacion", ex);
        String payload;
        String htmlTemplate = "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "</head>\n" +
                "<body>" +
                "%s" +
                "</body>" +
                "</html>";

        if (ex instanceof NotFoundWebElement notFoundWebElement) {
            List<String> selectorStrList = new ArrayList<>();
            List<SelectorType> selectors = notFoundWebElement.getSelectors();
            String id = notFoundWebElement.getId();
            for (int index = 0, selectorsSize = selectors.size(); index < selectorsSize; index++) {
                SelectorType selector = selectors.get(index);
                KindOfBy by = selector.getBy();
                String path = selector.getValue();
                selectorStrList.add((index + 1) + ".-" + by + ":" + path);
            }
            payload = "<h3>Selectores usados en la accion con el Id " + id + "</h3>";
            payload += "<ul><li>" + Joiner.on("</li><li>").skipNulls().join(selectorStrList) + "</li></ul>";
        } else if (ex instanceof NotFoundQaXmlFile notFoundQaXmlFile) {
            payload = "<h3>No fue encontrado el XML en la ruta asignada<h3>";
            payload += "<p>Descripcion: " + notFoundQaXmlFile.getDesc() + "<p>";
            payload += "<p>Archivo: " + notFoundQaXmlFile.getFile() + "<p>";
        } else if (ex instanceof NoQaRobotXmlValid noQaRobotXmlValid) {
            List<String> errorItems = noQaRobotXmlValid.getErrores().stream().map(err -> "<li>" + err + "</li>").toList();
            payload = "<h3>Error en la validacion del XML</h3>";
            payload += "<ul>" + Joiner.on(" ").join(errorItems) + "</ul>";
        } else {
            payload = "<p>" + ex.getMessage() + "</p>";
        }

        TitleIconAndMsgPojo errorMessage = TitleIconAndMsgPojo.create(
                "No se pudo completar la prueba",
                String.format(htmlTemplate, payload),
                MyOwnIcos.ERROR_MDPI
        );
        errorMessage.setDimension(new Dimension(400, 300));
        errorMessage.setClose(false);
        errorMessage.setRun(null);
        errorMessage.setRunInThread(true);

        this.sendInfo2MyDialog.onNext(errorMessage);
    }

    public void stateChanged(ChangeEvent e) {
        JSpinner jSpinner = (JSpinner) e.getSource();
        SpinnerModel model = jSpinner.getModel();
        Integer value = (Integer) model.getValue();
        interacion.onNext(value);
    }

    public void checkAction(ItemEvent itemEvent) {
        grabar.onNext(itemEvent.getStateChange() == ItemEvent.SELECTED);
    }

    public void pauseOrResumenButtonListener(ActionEvent ev) {
        PauseOrResumeState pauseOrResumeState;
        JToggleButton toggleButton = (JToggleButton) ev.getSource();
        if (toggleButton.isSelected()) {
            pauseOrResumeState = PauseOrResumeState.RESUME;
        } else {
            pauseOrResumeState = PauseOrResumeState.PAUSE;
        }
        pauseOrResumenActionExecution.onNext(pauseOrResumeState);
    }
}

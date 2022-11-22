package com.rebirth.qarobot.app.viewmodel;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.rebirth.qarobot.app.main.QAMaster;
import com.rebirth.qarobot.commons.exceptions.NoQaRobotXmlValid;
import com.rebirth.qarobot.commons.exceptions.NotFoundQaXmlFile;
import com.rebirth.qarobot.commons.exceptions.NotFoundWebElement;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.models.dtos.dialogs.MyOwnIcos;
import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.KindOfBy;
import com.rebirth.qarobot.commons.models.dtos.qarobot.SelectorType;
import com.rebirth.qarobot.commons.models.dtos.tables.ActionColorAndExIfExits;
import com.rebirth.qarobot.commons.models.dtos.toggle.PauseOrResumeState;
import com.rebirth.qarobot.scraping.enums.Browser;
import com.rebirth.qarobot.commons.models.dtos.QaRobotContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Data
@Singleton
public class MainViewModel {

    private final BehaviorSubject<List<BaseActionType>> actionDtos = BehaviorSubject.createDefault(Lists.newArrayList());
    private final BehaviorSubject<String> statusBar = BehaviorSubject.createDefault("Ready!");
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
    private final MessageDigest md5;
    private final AtomicReference<String> md5Storage = new AtomicReference<>();
    private final Configuracion configuracion;

    @Inject
    public MainViewModel(QAMaster qaMaster, MessageDigest md5, Configuracion configuracion) {
        this.qaMaster = qaMaster;
        this.md5 = md5;
        this.configuracion = configuracion;
    }

    public final MouseAdapter chooseXmlPathMouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            JButton button = (JButton) e.getSource();
            if (!button.isEnabled()) return;
            if (SwingUtilities.isLeftMouseButton(e)) {
                JFileChooser jFileChooser = new JFileChooser(configuracion.getXmlHome());
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Xmlenium Files", "xml");
                jFileChooser.setFileFilter(filter);
                int resultado = jFileChooser.showOpenDialog(null);
                if (resultado == JFileChooser.APPROVE_OPTION) {
                    try {
                        MainViewModel.this.statusBar.onNext("Cargando....");
                        File newXml = jFileChooser.getSelectedFile();
                        storageFileAndSetMd5(newXml);
                        loadXml2QaRobot();
                    } catch (NoQaRobotXmlValid noQaRobotXmlValid) {
                        MainViewModel.this.finishQaWithError(noQaRobotXmlValid);
                    }
                }
            }
        }

        private void storageFileAndSetMd5(File xml) {
            String md5Sing = this.getMd5FromFile(xml);
            md5Storage.set(md5Sing);
            selectedFile.onNext(xml);
            statusBar.onNext("XML Cargado y listo para iniciar! sha256 " + md5Sing);
        }

        private String getMd5FromFile(File xml) {
            try {
                HashCode hash = Files.asByteSource(xml).hash(Hashing.sha256());
                return hash.toString().toUpperCase(Locale.ROOT);
            } catch (IOException ioException) {
                return "";
            }
        }

    };

    public final MouseAdapter reloadQaRobotMouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            JButton button = (JButton) e.getSource();
            if (!button.isEnabled()) return;
            if (SwingUtilities.isLeftMouseButton(e)) {
                try {
                    loadXml2QaRobot();
                } catch (NoQaRobotXmlValid noQaRobotXmlValid) {
                    MainViewModel.this.finishQaWithError(noQaRobotXmlValid);
                }
            }
        }
    };


    private void loadXml2QaRobot() {
        File xmlFile = selectedFile.getValue();
        if (Objects.nonNull(xmlFile)) {
            qaMaster.readQaRobot(xmlFile);
        }
    }


    public final MouseAdapter initProcessorMouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            JButton button = (JButton) e.getSource();
            if (!button.isEnabled()) return;
            if (SwingUtilities.isLeftMouseButton(e)) {
                MainViewModel.this.statusInitButton.onNext(false);
                MainViewModel.this.statusReloadButton.onNext(false);
                MainViewModel.this.searchButton.onNext(false);
                MainViewModel.this.pauseOrResumenStatus.onNext(true);
                MainViewModel.this.cleanTableColor();
                new Thread(MainViewModel.this.qaMaster).start();
            }
        }
    };

    public void changeComboItem(ItemEvent itemEvent) {
        Browser currentBrowser = (Browser) itemEvent.getItem();
        this.browser.onNext(currentBrowser);
    }

    public void changeComboItemGraphicsDevice(ItemEvent itemEvent) {
        GraphicsDevice graphicsDevice = (GraphicsDevice) itemEvent.getItem();
        this.selectedScreen.onNext(graphicsDevice);
    }

    public void cleanTableColor() {
        List<BaseActionType> currentList = actionDtos.getValue();
        if (currentList != null && !currentList.isEmpty()) {
            for (int i = 0; i < currentList.size(); i++) {
                BaseActionType baseActionDto = currentList.get(i);
                Color color = i % 2 == 0 ? Color.LIGHT_GRAY : Color.WHITE;
                interaccionData2ChangeAdvanceInTable.onNext(ActionColorAndExIfExits.create(baseActionDto, color));
            }
        }
    }

    public void restartStatusButtons() {
        this.searchButton.onNext(true);
        this.statusInitButton.onNext(false);
        this.statusReloadButton.onNext(false);
        this.pauseOrResumenStatus.onNext(false);

    }

    public void finishQa() {
        restartStatusButtons();
        JOptionPane.showMessageDialog(null, "Prueba Terminada", "QA Flux", JOptionPane.INFORMATION_MESSAGE);
    }

    public void finishQaWithError(Throwable ex) {
        restartStatusButtons();
        statusBar.onNext("Error en la aplicacion: " + ex.getMessage());
        log.error("Error en la aplicacion", ex);
        String payload;
        String htmlTemplate = "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "</head>\n" +
                "<body style=\"background-color:lightgrey;\">" +
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
            payload = "<p style=\"text-align: justify; font-size: small; color: black; \">" + ex.getMessage() + "</p>";
        }

        TitleIconAndMsgPojo errorMessage = TitleIconAndMsgPojo.create(
                "Error!!",
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

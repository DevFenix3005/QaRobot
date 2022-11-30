package com.rebirth.qarobot.app.main;


import dagger.Lazy;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import jakarta.xml.bind.JAXBException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.rebirth.qarobot.app.utils.QaXmlReadService;
import com.rebirth.qarobot.app.viewmodel.MainViewModel;
import com.rebirth.qarobot.commons.exceptions.NoQaRobotXmlValid;
import com.rebirth.qarobot.commons.exceptions.NotFoundQaXmlFile;
import com.rebirth.qarobot.commons.models.dtos.QarobotWrapper;
import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;
import com.rebirth.qarobot.commons.models.dtos.toggle.PauseOrResumeState;
import com.rebirth.qarobot.commons.utils.ShowInDialog;
import com.rebirth.qarobot.record.di.RecordComponent;
import com.rebirth.qarobot.record.video.MyQAVideoRecorder;
import com.rebirth.qarobot.scraping.QaRobotXml;
import com.rebirth.qarobot.scraping.di.ScrappingComponent;
import com.rebirth.qarobot.scraping.enums.Browser;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import javax.xml.transform.TransformerException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Slf4j
@Data
@Singleton
public class QAMaster implements Runnable {

    private final Provider<ScrappingComponent.Factory> scrappingComponentProvider;
    private final Provider<RecordComponent.Factory> recordComponentProvider;
    private final QaXmlReadService qaXmlReadService;
    private QarobotWrapper qarobot;
    private Lazy<MainViewModel> mainViewModelLazy;

    @Inject
    public QAMaster(QaXmlReadService qaXmlReadService,
                    Provider<ScrappingComponent.Factory> scrappingComponentProvider,
                    Provider<RecordComponent.Factory> recordComponentProvider,
                    Lazy<MainViewModel> mainViewModelLazy
    ) {
        this.qaXmlReadService = qaXmlReadService;
        this.scrappingComponentProvider = scrappingComponentProvider;
        this.recordComponentProvider = recordComponentProvider;
        this.mainViewModelLazy = mainViewModelLazy;
    }

    public QaRobotXml getScrappingComponentProviderQaRobot() {
        MainViewModel mainViewModel = this.mainViewModelLazy.get();
        @Nullable Browser innerBrowser = mainViewModel.getBrowser().getValue();
        //GraphicsDevice value = mainViewModel.getSelectedScreen().getValue();
        return this.scrappingComponentProvider.get()
                .create(innerBrowser)
                .getQaRobotXml();
    }

    public MyQAVideoRecorder getRecordComponentProvider(File movieFolder) {
        MainViewModel mainViewModel = this.mainViewModelLazy.get();
        @Nullable GraphicsDevice value = mainViewModel.getSelectedScreen().getValue();
        return this.recordComponentProvider.get()
                .create(movieFolder, value)
                .getMyQAVideoRecorder();
    }

    public void readQaRobot(File xmlFile) {
        MainViewModel mainViewModel = this.mainViewModelLazy.get();
        try {
            this.qaXmlReadService.setQaXmlFile(xmlFile);
            this.qarobot = qaXmlReadService.read();
            if (this.qarobot.isValidXml()) {
                mainViewModel.getActionDtos().onNext(this.qarobot.getActions());
                mainViewModel.getXmlPath().onNext(xmlFile.getName());
                mainViewModel.getStatusInitButton().onNext(true);
                mainViewModel.getStatusReloadButton().onNext(true);
            } else {
                throw new NoQaRobotXmlValid(qarobot.getErrores());
            }
        } catch (IOException | TransformerException | NotFoundQaXmlFile | JAXBException e) {
            log.error("No se pudo leer correctamente el XML", e);
            mainViewModel.finishQaWithError(e);
        }
    }

    @Override
    public void run() {
        MainViewModel mainViewModel = this.mainViewModelLazy.get();
        BehaviorSubject<Boolean> grabarRx = mainViewModel.getGrabar();
        boolean innerRec = false;
        if (grabarRx != null) {
            Boolean grabarRxValue = grabarRx.getValue();
            innerRec = grabarRxValue != null && grabarRxValue;
        }
        int innerIteration = 1;

        Integer iteracionRx = mainViewModel.getInteracion().getValue();
        if (Objects.nonNull(iteracionRx)) {
            innerIteration = iteracionRx;
        }

        boolean finishSuccess = true;

        for (int i = 1; i <= innerIteration; i++) {
            mainViewModel.getStatusBar().onNext(String.format("Iniciando la iteracion numero %d de la prueba", i));

            try (QaRobotXml scrapping = this.getScrappingComponentProviderQaRobot()) {

                File dashboardExitDir = this.qarobot.getDashboardExitFile();
                if (!dashboardExitDir.exists()) {
                    dashboardExitDir.mkdirs();
                } else if (!this.qarobot.getDashboardExitFile().isDirectory()) {
                    throw new IOException("\"" + dashboardExitDir.getAbsolutePath() + "\" is not a directory.");
                }

                mainViewModel.getPauseOrResumenActionExecution()
                        .filter(state -> state != PauseOrResumeState.NONE)
                        .subscribe(pauseOrResumen -> {
                            if (pauseOrResumen == PauseOrResumeState.RESUME) {
                                scrapping.pauseExecution();
                            } else {
                                scrapping.resumenExecution();
                            }
                        });
                scrapping.setQaRobot(this.qarobot);
                scrapping.delegateSenders2SeleniumHelper(
                        data -> mainViewModel.getInteraccionData2ChangeAdvanceInTable().onNext(data),
                        new ShowInDialog() {
                            @Override
                            public void run(TitleIconAndMsgPojo titleIconAndMsgPojo) {
                                mainViewModel.getSendInfo2MyDialog().onNext(titleIconAndMsgPojo);
                            }

                            @Override
                            public void hideDialog() {
                                mainViewModel.getHideMyDialog().onNext(true);
                            }
                        },
                        () -> mainViewModel.getPauseOrResumenActionExecution().onNext(PauseOrResumeState.RESUME),
                        (qaRobotContext -> mainViewModel.getShowEvalTable().onNext(qaRobotContext))
                );

                if (innerRec) {
                    MyQAVideoRecorder myQAVideoRecorder = this.getRecordComponentProvider(dashboardExitDir);
                    myQAVideoRecorder.start();
                    finishSuccess &= scrapping.flux();
                    myQAVideoRecorder.stop();
                } else {
                    finishSuccess &= scrapping.flux();
                }
            } catch (IOException e) {
                mainViewModel.finishQaWithError(e);
                break;
            }

            if (i != innerIteration) {
                mainViewModel.cleanTableColor();
            }
        }
        if (finishSuccess) {
            mainViewModel.getHideMyDialog().onNext(true);
            mainViewModel.finishQa();
        }

        mainViewModel.getPauseOrResumenActionExecution().onNext(PauseOrResumeState.NONE);
        //return finishSuccess;
    }


}

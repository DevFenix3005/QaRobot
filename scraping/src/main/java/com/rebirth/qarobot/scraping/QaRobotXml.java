package com.rebirth.qarobot.scraping;

import com.rebirth.qarobot.commons.models.dtos.QarobotWrapper;
import com.rebirth.qarobot.commons.utils.PuaseExecutionFromStopAction;
import com.rebirth.qarobot.commons.utils.SendInfo2View;
import com.rebirth.qarobot.commons.utils.SendQaContext2View;
import com.rebirth.qarobot.commons.utils.ShowInDialog;

import java.io.Closeable;

public interface QaRobotXml extends Closeable {

    void setQaRobot(QarobotWrapper mainQaRobot);

    boolean flux();

    void delegateSenders2SeleniumHelper(SendInfo2View sendInfo2View, ShowInDialog showInDialog,
                                        PuaseExecutionFromStopAction puaseExecutionFromStopAction,
                                        SendQaContext2View qaContext2View);

    void pauseExecution();

    void resumenExecution();
}

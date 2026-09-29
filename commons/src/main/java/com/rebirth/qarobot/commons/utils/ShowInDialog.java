package com.rebirth.qarobot.commons.utils;

import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;

public interface ShowInDialog {

    void run(TitleIconAndMsgPojo titleIconAndMsgPojo);

    void hideDialog();
}

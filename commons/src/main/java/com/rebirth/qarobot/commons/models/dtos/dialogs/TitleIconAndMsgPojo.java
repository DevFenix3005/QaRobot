package com.rebirth.qarobot.commons.models.dtos.dialogs;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.awt.Dimension;
import java.awt.Point;

@Data()
@RequiredArgsConstructor(staticName = "create")
public class TitleIconAndMsgPojo {

    @NonNull
    private String title;
    @NonNull
    private String msg;
    @NonNull
    private MyOwnIcos myOwnIcos;

    private boolean close = false;
    private long closeAfter = 3500;
    private Point point;
    private Dimension dimension;

    private boolean runInThread;
    private Runnable run;

}

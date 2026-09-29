package com.rebirth.qarobot.commons.models.dtos.tables;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;

import java.awt.*;

@Data
@RequiredArgsConstructor(staticName = "create")
public class ActionColorAndExIfExits {

    @NonNull
    private BaseActionType baseActionType;
    @NonNull
    private Color color;

    private Throwable throwable;

}

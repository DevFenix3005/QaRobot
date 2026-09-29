package com.rebirth.qarobot.scraping.di.modules.action;

import com.rebirth.qarobot.commons.models.dtos.qarobot.*;
import com.rebirth.qarobot.scraping.di.annotations.ActionKey;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import com.rebirth.qarobot.scraping.models.qabot.actions.impl.*;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class ActionModule {

    private ActionModule() {
    }

    @Binds
    @IntoMap
    @ActionKey(ChooseActionType.class)
    public abstract Action<? extends BaseActionType> chooseActionBind(ChooseAction chooseAction);

    @Binds
    @IntoMap
    @ActionKey(ClickActionType.class)
    public abstract Action<? extends BaseActionType> clickActionBind(ClickAction clickAction);

    @Binds
    @IntoMap
    @ActionKey(DelayActionType.class)
    public abstract Action<? extends BaseActionType> delayActionBind(DelayAction delayAction);

    @Binds
    @IntoMap
    @ActionKey(ReadActionType.class)
    public abstract Action<? extends BaseActionType> readActionBind(ReadAction readAction);

    @Binds
    @IntoMap
    @ActionKey(RestActionType.class)
    public abstract Action<? extends BaseActionType> restActionBind(RestAction restAction);

    @Binds
    @IntoMap
    @ActionKey(StopActionType.class)
    public abstract Action<? extends BaseActionType> stopActionBind(StopAction stopAction);

    @Binds
    @IntoMap
    @ActionKey(SwitchtabActionType.class)
    public abstract Action<? extends BaseActionType> switchTabActionBind(SwitchTabAction switchTabAction);

    @Binds
    @IntoMap
    @ActionKey(VerifyActionType.class)
    public abstract Action<? extends BaseActionType> verifyActionBind(VerifyAction verifyAction);

    @Binds
    @IntoMap
    @ActionKey(WriteActionType.class)
    public abstract Action<? extends BaseActionType> writeActionBind(WriteAction writeAction);

    @Binds
    @IntoMap
    @ActionKey(ScriptActionType.class)
    public abstract Action<? extends BaseActionType> scriptActionBind(ScriptAction scriptAction);

    @Binds
    @IntoMap
    @ActionKey(OpenActionType.class)
    public abstract Action<? extends BaseActionType> openActionBind(OpenAction scriptAction);

    @Binds
    @IntoMap
    @ActionKey(MessageActionType.class)
    public abstract Action<? extends BaseActionType> messageActionBind(MessageAction messageAction);

    @Binds
    @IntoMap
    @ActionKey(IterationActionType.class)
    public abstract Action<? extends BaseActionType> iterationActionBind(IterationAction iterationAction);


}

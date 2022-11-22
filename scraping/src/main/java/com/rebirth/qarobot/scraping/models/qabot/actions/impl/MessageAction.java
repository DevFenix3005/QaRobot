package com.rebirth.qarobot.scraping.models.qabot.actions.impl;

import com.rebirth.qarobot.scraping.SeleniumHelper;
import com.rebirth.qarobot.scraping.models.qabot.actions.Action;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.commons.di.annotations.scopes.ChildComponent;
import com.rebirth.qarobot.commons.models.dtos.dialogs.MyOwnIcos;
import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;
import com.rebirth.qarobot.commons.models.dtos.qarobot.MessageActionType;

import javax.inject.Inject;
import java.awt.*;

@Log4j2
@ChildComponent
@EqualsAndHashCode(callSuper = true)
public class MessageAction extends Action<MessageActionType> {

    @Inject
    public MessageAction(SeleniumHelper seleniumHelper) {
        super(seleniumHelper);
    }

    @Override
    public void execute() {
        String body = "<h2>" + this.actionDto.getMsg() + "</h2><br/>";
        body += "<h5>" + this.actionDto.getDesc() + "</h5>";
        String htmlTemplate = this.seleniumHelper.createHtml(body);
        TitleIconAndMsgPojo titleIconAndMsgPojo = TitleIconAndMsgPojo.create("Mensaje", htmlTemplate, MyOwnIcos.ROBOT_MDPI);
        titleIconAndMsgPojo.setDimension(new Dimension(800, 600));
        titleIconAndMsgPojo.setPoint(this.seleniumHelper.seleniumPoint2AwtPoint());
        titleIconAndMsgPojo.setCloseAfter(0);
        titleIconAndMsgPojo.setRunInThread(false);
        titleIconAndMsgPojo.setClose(false);
        this.seleniumHelper.displayDialog(titleIconAndMsgPojo);
        this.seleniumHelper.delay(actionDto.getTimeout().longValue());
        this.seleniumHelper.hideDialog();
    }

}

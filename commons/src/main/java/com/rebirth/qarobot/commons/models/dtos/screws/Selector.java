package com.rebirth.qarobot.commons.models.dtos.screws;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Data;

import java.io.Serializable;

@Data
public class Selector implements Serializable {

    @JacksonXmlText
    private String path;

}

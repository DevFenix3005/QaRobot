package com.rebirth.qarobot.commons.models.dtos.screws;

import tools.jackson.dataformat.xml.annotation.JacksonXmlCData;
import tools.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TextBody implements Serializable {

    @JacksonXmlText
    @JacksonXmlCData
    private String body;


    @Override
    public String toString() {
        return body;
    }
}

package com.rebirth.qarobot.commons;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rebirth.qarobot.commons.models.dtos.screws.Item;
import com.rebirth.qarobot.commons.models.dtos.screws.Selector;
import com.rebirth.qarobot.commons.models.dtos.screws.SetValue;
import com.rebirth.qarobot.commons.models.dtos.screws.TextBody;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class JacksonSerializationTest {
    @Test
    void jsonSupportsSharedAnnotationsAndJavaTimeWithoutTheOldJsr310Module() {
        JsonMapper mapper = JsonMapper.builder().build();
        RunDetails details = new RunDetails("Prueba ñ", LocalDateTime.of(2026, 9, 28, 12, 30));

        String json = mapper.writeValueAsString(details);

        assertTrue(json.contains("\"started_at\":\"2026-09-28T12:30:00\""));
        assertEquals(details, mapper.readValue(json, RunDetails.class));
    }

    @Test
    void yamlRoundTripsTheExistingValueDto() {
        YAMLMapper mapper = YAMLMapper.builder().build();
        SetValue value = new SetValue("token", "café: prueba #1");

        assertEquals(value, mapper.readValue(mapper.writeValueAsString(value), SetValue.class));
    }

    @Test
    void xmlRetainsAttributesOnExistingDtos() {
        XmlMapper mapper = XmlMapper.builder().build();
        SetValue value = new SetValue("token", "a&b");

        String xml = mapper.writeValueAsString(value);

        assertTrue(xml.contains("key=\"token\""));
        assertTrue(xml.contains("value=\"a&amp;b\""));
        assertEquals(value, mapper.readValue(xml, SetValue.class));
        assertEquals(new Item("demo"), mapper.readValue("<Item value=\"demo\"/>", Item.class));
    }

    @Test
    void xmlRetainsTextContentAndCdata() {
        XmlMapper mapper = XmlMapper.builder().build();
        TextBody body = new TextBody("if (a < b && b > 0) return 'ñ';");

        String xml = mapper.writeValueAsString(body);

        assertTrue(xml.contains("<![CDATA["));
        assertEquals(body, mapper.readValue(xml, TextBody.class));
        Selector selector = mapper.readValue("<Selector>//input[@id='name']</Selector>", Selector.class);
        assertEquals("//input[@id='name']", selector.getPath());
    }

    public record RunDetails(String name, @JsonProperty("started_at") LocalDateTime startedAt) {
    }
}

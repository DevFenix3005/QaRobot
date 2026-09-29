package com.rebirth.qarobot.scraping.utils;

import kong.unirest.GenericType;
import kong.unirest.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Connects Unirest 3's mapper interface to Jackson 3 without its Jackson 2 adapter. */
public final class UnirestJacksonMapper implements ObjectMapper {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public <T> T readValue(String value, Class<T> valueType) {
        return mapper.readValue(value, valueType);
    }

    @Override
    public <T> T readValue(String value, GenericType<T> genericType) {
        return mapper.readValue(value, mapper.constructType(genericType.getType()));
    }

    @Override
    public String writeValue(Object value) {
        return mapper.writeValueAsString(value);
    }
}

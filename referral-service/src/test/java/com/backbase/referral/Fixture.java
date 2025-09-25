package com.backbase.referral;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;

public interface Fixture {

    ObjectMapper mapper = json().findModulesViaServiceLoader(true).build();

    static <T> T resourceJson(String fileName, Class<T> clazz) {
        try {
            return mapper.readValue(Fixture.class.getClassLoader().getResource(fileName), clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

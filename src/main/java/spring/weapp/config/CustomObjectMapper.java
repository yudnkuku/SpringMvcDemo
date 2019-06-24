package spring.weapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class CustomObjectMapper extends ObjectMapper {

    public CustomObjectMapper() {
        super();
        configure(SerializationFeature.INDENT_OUTPUT, true);
    }
}

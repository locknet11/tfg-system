package com.spulido.tfg.common;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).registerModule(new JavaTimeModule());

    public static String objectToJson(Object arg) throws JsonProcessingException {
        return OBJECT_MAPPER.setDateFormat(new SimpleDateFormat("MM/dd/yy hh:mm a")).writeValueAsString(arg);
    }

    public static String objectToJsonWithDateFormat(Object arg, String format) throws JsonProcessingException {
        return new ObjectMapper()
                .setDateFormat(new SimpleDateFormat(format))
                .writeValueAsString(arg);
    }

    public static <T> T jsonToObject(String json, Class<T> arg) throws IOException {
        return OBJECT_MAPPER.readValue(json, arg);
    }

    public static <C extends Collection<T>, T> C jsonToCollection(String json, Class<C> arg0, Class<T> arg1)
            throws IOException {
        TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();
        return OBJECT_MAPPER.readValue(json, typeFactory.constructCollectionType(arg0, arg1));
    }

    public static <T> T jsonToGenericObject(String json, TypeReference<T> valueTypeRef) throws IOException {
        return OBJECT_MAPPER.readValue(json, valueTypeRef);
    }

    public static <T> T jsonToArray(String json, Class<T> arr) throws IOException {
        return new ObjectMapper()
                .configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true)
                .readValue(json, arr);
    }

    public static String getNode(String json, String node) throws IOException {
        JsonNode jsonNode = OBJECT_MAPPER.readTree(json);
        return jsonNode.get(node).toString();
    }
}

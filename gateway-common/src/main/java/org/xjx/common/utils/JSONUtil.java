package org.xjx.common.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

public class JSONUtil {
    /**
     *  response 序列化参数
     */
    public static final String CODE = "code";
    public static final String STATUS = "status";
    public static final String DATA = "data";
    public static final String MESSAGE = "message";

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final JsonFactory jasonFactory = mapper.getFactory();

    static {
        // 序列化时间配置
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"));
        // 反序列化出现未定义字段时不报错
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // dubbo 泛化调用
    }

    /**
     * Object——>JSON
     * @param obj
     * @return
     */
    public static String toJSONString(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("object format to json error:"+obj,e);
        }
    }

    public static void outputToWriter(Writer out, Object value) {
        try {
            mapper.writeValue(out, value);
        } catch (IOException e) {
            throw new RuntimeException("output to write error:"+value, e);
        }
    }

    public static <T> T parse(JsonNode body, Class<T> clazz) {
        try {
            return mapper.readValue(body.traverse(), clazz);
        } catch (IOException e) {
            throw new RuntimeException("json node parse to object [" + clazz + "] error:" + body, e);
        }
    }

    public static <T> T parse(String str, Class<T> clazz) {
        try {
            return mapper.readValue(str == null ? "" : str, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("json node parse to object [" + clazz + "] error:" + str, e);
        }
    }

    public static <T> T parse(Optional<String> json, Class<T> clazz) {
        return json.map((str) -> parse(str, clazz)).orElse(null);
    }

    public static <T> T parse(String str, TypeReference<T> tr) {
        try {
            return mapper.readValue(str, tr);
        } catch (Exception e) {
            throw new RuntimeException("json parse to object [" + tr + "] error:" + str, e);
        }
    }

    public static <T> T parse(JsonNode body, JavaType javaType) {
        try {
            return mapper.readValue(body.traverse(), javaType);
        } catch (IOException e) {
            throw new RuntimeException("json parse to object [" + body + "] error:" + body, e);
        }
    }

    public static <T> T parse(String str, JavaType javaType) {
        try {
            return mapper.readValue(str, javaType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("json parse to Object [" + str + "] error:" + str, e);
        }
    }

    /**
     * String——>List<T>
     * @param json
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> parseToList(String json, Class<T> clazz) {
        return parse(json, getCollectionType(List.class, clazz));
    }

    public static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return mapper.getTypeFactory()
                .constructParametricType(collectionClass, elementClasses);
    }

    public static JsonNode tree(String json) {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("object format to json error:" + json, e);
        }
    }

    public static ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }

    /**
     * 解析某字段值
     * @param json
     * @param fieldName
     * @return
     */
    public static String parseOneField(String json, String fieldName) {
        try {
            JsonParser parser = jasonFactory.createParser(json);
            while(parser.nextToken() != JsonToken.END_OBJECT) {
                String name = parser.getCurrentName();
                if (name.equals(fieldName)) {
                    // move to next token
                    parser.nextToken();
                    return parser.getText();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("object format to json error:", e);
        }
        return null;
    }
}

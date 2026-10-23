package app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Zi JunJie <a href="https://github.com/zjjfly"/>github</a>
 */
@Slf4j
public class JsonUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    @SneakyThrows
    public static <T> T readValue(String json, TypeReference<T> tTypeReference) {
        return MAPPER.readValue(json, tTypeReference);
    }

    @SneakyThrows
    public static <T> String toJsonString(T t) {
        return MAPPER.writeValueAsString(t);
    }

    private JsonUtil() {
        throw new IllegalStateException("JsonUtil utility class");
    }

    public static <T> T convertValue(Object object, Class<T> clazz) {
        return MAPPER.convertValue(object, clazz);
    }

}

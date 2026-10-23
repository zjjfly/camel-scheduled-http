package app.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.camel.component.http.HttpMethods;

import java.util.Map;

/**
 * @author Zi JunJie <a href="https://github.com/zjjfly"/>github</a>
 */
@Data
@Accessors(chain = true)
public class RequestOption {

    private boolean secure;

    private String host;

    private Integer port;

    private HttpMethods method;

    private String bodyTemplate;

    private Map<String, String> headers;

    private String path;

    @JsonIgnore
    public String getUrl() {
        return String.format("%s://%s:%d%s", secure ? "https" : "http", host, port == null ? (secure ? 443 : 80) : port, path == null ? "" : path);
    }

}

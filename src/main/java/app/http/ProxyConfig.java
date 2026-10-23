package app.http;

import lombok.Data;

/**
 * @author Zi JunJie <a href="https://github.com/zjjfly"/>github</a>
 */
@Data
public class ProxyConfig {

    private boolean secure;

    private String host;

    private Integer port;

    private String user;

    private String password;

}

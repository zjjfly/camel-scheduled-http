package app.http;

import lombok.Data;

/**
 * @author Zi JunJie <a href="https://github.com/zjjfly"/>github</a>
 */
@Data
public class ClientOption {

    private int requestTimeout;

    private boolean secure;

    private ProxyConfig proxyConfig;

    private SslConfig sslConfig;

}

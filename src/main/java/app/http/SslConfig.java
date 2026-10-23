package app.http;

import lombok.Data;

/**
 * @author Zi JunJie <a href="https://github.com/zjjfly"/>github</a>
 */
@Data
public class SslConfig {

    private String resource;

    private String keyPassword;

    private String keyStorePassword;

}

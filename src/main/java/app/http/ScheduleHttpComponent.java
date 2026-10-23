package app.http;

import app.JsonUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Endpoint;
import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.camel.component.http.ProxyHttpClientConfigurer;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.util.URISupport;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Zi JunJie <a href="https://github.com/zjjfly"/>github</a>
 */
@Slf4j
public class ScheduleHttpComponent extends DefaultComponent {

    public static final String COMPONENT_NAME = "shttp";

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws URISyntaxException {
        ClientOption clientOption = new ClientOption();
        if (parameters.containsKey(ConfigFields.CLIENT_OPTION)) {
            String clientOptionsJson = MapUtil.getStr(parameters, ConfigFields.CLIENT_OPTION);
            parameters.remove(ConfigFields.CLIENT_OPTION);
            if (StrUtil.isNotBlank(clientOptionsJson)) {
                clientOption = JsonUtil.readValue(Base64.decodeStr(clientOptionsJson), new TypeReference<ClientOption>() {
                });
            }
        }

        HttpClientBuilder clientBuilder = createHttpClientBuilder(clientOption);
        clientBuilder.setServiceUnavailableRetryStrategy(new DefaultServiceUnavailableRetryStrategy(3,10));
        HttpClientConnectionManager clientConnectionManager = createConnectionManager(clientOption);
        clientBuilder.setConnectionManager(clientConnectionManager);
        clientBuilder.setConnectionManagerShared(false);
        HttpClientConfigurer httpClientConfigurer = configureHttpProxy(clientOption);
        String query = URISupport.createQueryString(parameters, false);
        return new ScheduleHttpEndpoint(String.format("%s://%s?%s", COMPONENT_NAME, remaining, query), this, clientBuilder, httpClientConfigurer);
    }

    private SSLContext createSslContext(ClientOption clientOption) {
        if (!clientOption.isSecure()) {
            return null;
        }
        SslConfig sslConfig = clientOption.getSslConfig();
        try {
            //if ssl config is null,return a ssl context accepts self-signed certificates
            if (sslConfig == null) {
                return SSLContexts.custom()
                        .loadTrustMaterial(new TrustSelfSignedStrategy())
                        .build();
            }

            KeyStoreParameters ksp = new KeyStoreParameters();
            ksp.setResource(sslConfig.getResource());
            ksp.setPassword(sslConfig.getKeyStorePassword());
            KeyManagersParameters kmp = new KeyManagersParameters();
            kmp.setKeyStore(ksp);
            kmp.setKeyPassword(sslConfig.getKeyPassword());
            SSLContextParameters scp = new SSLContextParameters();
            scp.setKeyManagers(kmp);
            return scp.createSSLContext(getCamelContext());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ssl context");
        }
    }

    protected HttpClientBuilder createHttpClientBuilder(ClientOption clientOption) {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        requestConfigBuilder.setSocketTimeout(clientOption.getRequestTimeout());
        requestConfigBuilder.setConnectTimeout(0);
        requestConfigBuilder.setConnectionRequestTimeout(0);
        clientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());
        clientBuilder.disableRedirectHandling();
        clientBuilder.disableCookieManagement();
        return clientBuilder;
    }

    protected HttpClientConnectionManager createConnectionManager(ClientOption clientOption) {
        SSLContext sslContext = createSslContext(clientOption);
        boolean verifyHost = true;
        if (clientOption.isSecure() && clientOption.getSslConfig() == null) {
            verifyHost = false;
        }
        // create the default connection registry to use
        RegistryBuilder<ConnectionSocketFactory> builder = RegistryBuilder.create();
        builder.register("http", PlainConnectionSocketFactory.getSocketFactory());
        if (sslContext != null) {
            builder.register("https",
                    new SSLConnectionSocketFactory(sslContext, verifyHost ? new DefaultHostnameVerifier() : new NoopHostnameVerifier()));
        }
        return createConnectionManager(builder.build());
    }

    protected HttpClientConnectionManager createConnectionManager(Registry<ConnectionSocketFactory> registry) {
        // set up the connection live time
        PoolingHttpClientConnectionManager answer = new PoolingHttpClientConnectionManager(
                registry, null, null, null, Duration.ofMinutes(10).toMillis(), TimeUnit.MILLISECONDS);
        answer.setMaxTotal(100);
        answer.setDefaultMaxPerRoute(5);
        log.debug("Created ClientConnectionManager {}", answer);
        return answer;
    }

    private HttpClientConfigurer configureHttpProxy(ClientOption clientOption) {
        ProxyConfig proxyConfig = clientOption.getProxyConfig();
        if (proxyConfig == null) {
            proxyConfig = new ProxyConfig();
        }
        String proxyAuthScheme = proxyConfig.isSecure() ? "https" : "http";
        String proxyAuthHost = proxyConfig.getHost();
        Integer proxyAuthPort = proxyConfig.getPort();

        if (proxyAuthHost != null && proxyAuthPort != null) {
            String proxyAuthUsername = proxyConfig.getUser();
            String proxyAuthPassword = proxyConfig.getPassword();

            log.debug("Configuring HTTP client to use HTTP proxy {}:{}", proxyAuthHost, proxyAuthPort);

            if (proxyAuthUsername != null && proxyAuthPassword != null) {
                return new ProxyHttpClientConfigurer(
                        proxyAuthHost, proxyAuthPort, proxyAuthScheme, proxyAuthUsername, proxyAuthPassword,
                        null, null);
            } else {
                return new ProxyHttpClientConfigurer(
                        proxyAuthHost, proxyAuthPort, proxyAuthScheme);
            }
        }

        return null;
    }

}

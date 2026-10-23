package app;

import app.http.ClientOption;
import app.http.ProxyConfig;
import app.http.RequestOption;
import app.http.ScheduleHttpComponent;
import cn.hutool.core.codec.Base64;
import lombok.RequiredArgsConstructor;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpMethods;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;

import static app.http.ConfigFields.CLIENT_OPTION;
import static app.http.ConfigFields.REQUEST_OPTION;
import static app.http.ScheduleHttpComponent.COMPONENT_NAME;

/**
 * @author Zi JunJie(junjie.zi@siemens.com)
 */
@Service
@RequiredArgsConstructor
public class TestRouter extends RouteBuilder {

    private final CamelContext camelContext;

    @Override
    public void configure() {
        ClientOption clientOption = new ClientOption();
        clientOption.setSecure(true);
        clientOption.setRequestTimeout((int) Duration.ofSeconds(30).toMillis());
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setSecure(false);
        proxyConfig.setHost("127.0.0.1");
        proxyConfig.setPort(7890);
        clientOption.setProxyConfig(proxyConfig);
        camelContext.addComponent(COMPONENT_NAME, new ScheduleHttpComponent());
        String clientOptJson = Base64.encode(JsonUtil.toJsonString(clientOption));

        HashMap<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        RequestOption requestOption = new RequestOption()
                .setSecure(true)
                .setHost("www.google.com")
                .setMethod(HttpMethods.GET)
                .setPath("")
                .setHeaders(headers);
        String requestOptJson = Base64.encode(JsonUtil.toJsonString(requestOption));
        from(String.format("%s://foo?%s=%s&%s=%s&scheduler=quartz&scheduler.cron=0/10+*+*+*+*+?", COMPONENT_NAME, CLIENT_OPTION, clientOptJson, REQUEST_OPTION, requestOptJson))
                .to("log:foo");
    }
}

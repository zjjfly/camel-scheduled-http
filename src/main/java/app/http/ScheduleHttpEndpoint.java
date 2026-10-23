package app.http;

import app.JsonUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.camel.support.ScheduledPollEndpoint;
import org.apache.camel.util.URISupport;
import org.apache.camel.util.UnsafeUriCharactersEncoder;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static app.http.ConfigFields.REQUEST_OPTION;

/**
 * @author Zi JunJie <a href="https://github.com/zjjfly"/>github</a>
 */
@Slf4j
public class ScheduleHttpEndpoint extends ScheduledPollEndpoint {

    private final String endPointUri;

    private HttpClient httpClient;

    private final HttpClientBuilder clientBuilder;

    private final HttpClientConfigurer httpClientConfigurer;

    private final Map<String, Object> parameters;

    public ScheduleHttpEndpoint(String endPointURI, ScheduleHttpComponent component, HttpClientBuilder clientBuilder, HttpClientConfigurer httpClientConfigurer) throws URISyntaxException {
        super(endPointURI, component);
        this.endPointUri = endPointURI;
        this.clientBuilder = clientBuilder;
        this.httpClientConfigurer = httpClientConfigurer;
        int n = endPointUri.indexOf("?");
        parameters = new HashMap<>();
        if (n > 0) {
            Map<String, Object> query = URISupport.parseQuery(endPointURI.substring(n + 1));
            if (!query.containsKey(REQUEST_OPTION)) {
                throw new IllegalArgumentException(String.format("%s is not found in parameters", REQUEST_OPTION));
            }
            String requestOptJson = MapUtil.getStr(query, REQUEST_OPTION);
            RequestOption requestOption = JsonUtil.readValue(Base64.decodeStr(requestOptJson), new TypeReference<RequestOption>() {
            });
            query.put(REQUEST_OPTION, requestOption);
            parameters.putAll(query);
        }
    }

    @Override
    public Producer createProducer() {
        throw new UnsupportedOperationException("Producer is not supported");
    }

    public HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = createHttpClient();
        }
        return httpClient;
    }

    private HttpClient createHttpClient() {
        if (httpClientConfigurer != null) {
            httpClientConfigurer.configureHttpClient(clientBuilder);
        }
        log.debug("Setup the HttpClientBuilder {}", clientBuilder);
        return clientBuilder.build();

    }

    @Override
    protected String createEndpointUri() {
        return "schedule-http";
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        RequestOption requestOption = MapUtil.get(parameters, REQUEST_OPTION, RequestOption.class);
        ScheduleHttpPollingConsumer scheduleHttpPollingConsumer = new ScheduleHttpPollingConsumer(this, processor, requestOption);
        String encodedUri = UnsafeUriCharactersEncoder.encode(endPointUri);
        URI u = new URI(encodedUri);
        String query;
        int idx = endPointUri.indexOf('?');
        if (idx > -1) {
            query = endPointUri.substring(idx + 1);
        } else {
            query = u.getRawQuery();
        }
        Map<String, Object> options = URISupport.parseQuery(query, true);
        configureScheduledPollConsumerProperties(options);
        configureConsumer(scheduleHttpPollingConsumer);
        scheduleHttpPollingConsumer.setSendEmptyMessageWhenIdle(false);
        return scheduleHttpPollingConsumer;
    }

    @Override
    public ScheduleHttpComponent getComponent() {
        return (ScheduleHttpComponent) super.getComponent();
    }

    @Override
    public boolean isLenientProperties() {
        return true;
    }
}

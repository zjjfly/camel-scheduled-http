package app.http;

import app.JsonUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.http.HttpDeleteWithBodyMethod;
import org.apache.camel.component.http.HttpGetWithBodyMethod;
import org.apache.camel.http.base.HttpHelper;
import org.apache.camel.support.ScheduledPollConsumer;
import org.apache.camel.util.UnsafeUriCharactersEncoder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A scheduled HTTP polling consumer based on {@link org.apache.camel.component.http.HttpPollingConsumer}
 * with some enhancement
 *
 * @author Zi JunJie <a href="https://github.com/zjjfly"/>github</a>
 */
@Slf4j
public class ScheduleHttpPollingConsumer extends ScheduledPollConsumer {

    private final ScheduleHttpEndpoint endpoint;

    private final HttpClient httpClient;

    private final RequestOption requestOption;

    public ScheduleHttpPollingConsumer(ScheduleHttpEndpoint endpoint, Processor processor, RequestOption requestOption) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        this.httpClient = endpoint.getHttpClient();
        this.requestOption = requestOption;
    }

    protected Map<String, Object> getRequestParameters() {
        return null;
    }

    protected void doPoll() {
        Exchange exchange = endpoint.createExchange();
        HttpRequestBase method = createMethod(getRequestParameters());

        HttpEntity responseEntity = null;
        try {
            // execute request
            HttpResponse response = executeMethod(method);
            int responseCode = response.getStatusLine().getStatusCode();
            //check status code
            if (!HttpHelper.isStatusCodeOk(responseCode, "200-299")) {
                this.handleException(new RuntimeException("Error response"));
            }
            responseEntity = response.getEntity();
            if (responseEntity == null || responseEntity.getContent() == null) {
                this.handleException(new RuntimeException("Null response body"));
            }
            String respBody = IoUtil.read(responseEntity.getContent(), StandardCharsets.UTF_8);
            if (StrUtil.isBlank(respBody)) {
                this.handleException(new RuntimeException("Blank response body"));
            }
            Message message = exchange.getMessage();
            message.setBody(respBody);

            message.setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);
            if (response.getStatusLine() != null) {
                message.setHeader(Exchange.HTTP_RESPONSE_TEXT, response.getStatusLine().getReasonPhrase());
            }

            getProcessor().process(exchange);
        } catch (IOException e) {
            throw new RuntimeCamelException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (responseEntity != null) {
                try {
                    EntityUtils.consume(responseEntity);
                } catch (IOException e) {
                    // nothing what we can do
                }
            }
        }
    }

    /**
     * Strategy when executing the method (calling the remote server).
     *
     * @param httpRequest the http Request to execute
     * @return the response
     * @throws IOException can be thrown
     */
    protected HttpResponse executeMethod(HttpRequestBase httpRequest) throws IOException {
        return httpClient.execute(httpRequest);
    }

    // Implementation methods
    //-------------------------------------------------------------------------

    protected HttpRequestBase createMethod(Map<String, Object> params) {
        String url = requestOption.getUrl();
        url = processTemplate(url, params);
        String uri = UnsafeUriCharactersEncoder.encodeHttpURI(url);
        HttpRequestBase httpRequest;
        String bodyTemplate = requestOption.getBodyTemplate();
        boolean blankBodyTempalte = StrUtil.isBlank(bodyTemplate);
        switch (requestOption.getMethod()) {
            case GET:
                httpRequest = blankBodyTempalte ?
                        new HttpGet(uri) :
                        new HttpGetWithBodyMethod(uri, null);
                break;
            case DELETE:
                httpRequest = blankBodyTempalte ?
                        new HttpDelete(uri) :
                        new HttpDeleteWithBodyMethod(uri, null);
                break;
            case POST:
                httpRequest = new HttpPost(uri);
                break;
            case PUT:
                httpRequest = new HttpPut(uri);
                break;
            case HEAD:
                httpRequest = new HttpHead(uri);
                break;
            case PATCH:
                httpRequest = new HttpPatch(uri);
                break;
            case TRACE:
                httpRequest = new HttpTrace(uri);
                break;
            case OPTIONS:
                httpRequest = new HttpOptions(uri);
                break;
            default:
                throw new UnsupportedOperationException("This HTTP Method is not supported: " + requestOption.getMethod());
        }
        if (httpRequest instanceof HttpEntityEnclosingRequestBase) {
            if (!blankBodyTempalte) {
                ((HttpEntityEnclosingRequestBase) httpRequest)
                        .setEntity(new StringEntity(processTemplate(bodyTemplate, params), StandardCharsets.UTF_8));
            }
        }
        String headerTemplate = JsonUtil.toJsonString(requestOption.getHeaders());
        if (StrUtil.isBlank(headerTemplate)) {
            return httpRequest;
        }
        String headerStr = processTemplate(headerTemplate, params);
        if (StrUtil.isNotBlank(headerStr)) {
            Map<String, String> headers = JsonUtil.readValue(headerStr, new TypeReference<Map<String, String>>() {
            });
            headers.forEach(httpRequest::addHeader);
        }
        return httpRequest;
    }

    @Override
    protected int poll() {
        try {
            doPoll();
        } catch (Exception e) {
            log.error("Failed to pull message: ", e);
            return 0;
        }
        return 1;
    }

    public static String processTemplate(String templateStr, Map<String, Object> params) {
        if (StrUtil.isBlank(templateStr)) {
            return StrUtil.EMPTY;
        }
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_31);
        configuration.setNumberFormat("computer");
        try (StringWriter stringWriter = new StringWriter()) {
            Template template = new Template("" + templateStr.hashCode(), templateStr, configuration);
            template.process(params, stringWriter);
            return stringWriter.toString();
        } catch (IOException e) {
            throw new RuntimeException("Loading template failed: " + e.getMessage());
        } catch (TemplateException e) {
            throw new RuntimeException("Processing template failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Map<String, Object> params = new HashMap<>();
        params.put("pageSize", 10);
        params.put("pageIndex", 1);
        params.put("corpAccessToken", "xxxx");
        ArrayList<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        params.put("data.dataList.$._id", list);
        String template = "{    \"data\": {        \"dataObjectApiName\": \"%s\",        \"search_query_info\": {            \"limit\": ${pageSize},            \"offset\": ${pageSize * pageIndex},            \"filters\": [            <#if (data\\.dataList\\.$\\._id)?is_string>              {                \"operator\": \"EQ\",                \"field_name\": \"_id\",                \"field_values\": \"${data\\.dataList\\.$\\._id}\"              }            </#if>            <#if (data\\.dataList\\.$\\._id)?is_enumerable>              {                \"operator\": \"IN\",                \"field_name\": \"_id\",                \"field_values\": [                <#list (data\\.dataList\\.$\\._id) as item>                    \"${item}\"                <#sep>,                </#list>                ]              }            </#if>            ]        }    },    \"corpId\": \"FSAID_131c432\",    \"corpAccessToken\": \"${corpAccessToken}\",    \"currentOpenUserId\": \"FSUID_3709284598899095E2C75110BB1519C8\"}";
        System.out.println(template);
        System.out.println(processTemplate(template, params));
    }

}

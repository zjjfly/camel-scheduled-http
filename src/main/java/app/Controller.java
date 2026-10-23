package app;

import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Zi JunJie(junjie.zi@siemens.com)
 */
@Slf4j
@RestController
@RequestMapping("/produce")
public class Controller {

    private final CamelContext context;

    private FluentProducerTemplate grpcProducerTemplate;

    private FluentProducerTemplate thriftProducerTemplate;

    public Controller(CamelContext context) {
        this.context = context;
    }

//    @EventListener(ApplicationReadyEvent.class)
//    public void init() {
//        this.grpcProducerTemplate = DefaultFluentProducerTemplate.on(context)
//            .withProcessor(exchange -> {
//                exchange.getMessage().setBody(Empty.newBuilder().build());
//            }).to(String.format(
//                "grpc:localhost:%d/com.github.zjjfly.cd.api.grpc.MessageService?method=pull",
//                Constant.RAW_GRPC_SERVER_PORT));
//        this.thriftProducerTemplate = DefaultFluentProducerTemplate.on(context)
//            .to(String.format(
//                "thrift:localhost:%d/com.github.zjjfly.cd.api.thrift.MessageService?method=push",
//                Constant.RAW_THRIFT_SERVER_PORT));
//    }

//    @GetMapping("/grpc/pull")
//    public void grpcPull() {
//        log.info("pull response: " + grpcProducerTemplate.request());
//    }

//    @GetMapping("/thrift/push")
//    public void thriftPush() {
//        List<Object> body = new ArrayList<>();
//        List<Message> data = Lists.newArrayList();
//        for (int i = 0; i < 5; i++) {
//            Message message = new Message();
//            message.setData(ByteBuffer.wrap(String.format("{\n"
//                + "  \"id\": %d,\n"
//                + "  \"name\": \"name-%d\"\n"
//                + "}", i, i).getBytes(StandardCharsets.UTF_8)));
//            data.add(message);
//        }
//        body.add(data);
//        Object response = thriftProducerTemplate.withBody(body)
//            .request();
//        log.info("push response: " + response);
//    }

    @GetMapping("/test")
    public String test() {
        int i = RandomUtil.randomInt();
        if(i % 2 ==0){
            return "Hello";
        }
        throw new RuntimeException("error");
    }

}

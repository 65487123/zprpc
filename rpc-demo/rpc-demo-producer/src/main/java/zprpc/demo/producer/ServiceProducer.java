package zprpc.demo.producer;


import com.lzp.annotation.Service;
import org.springframework.stereotype.Component;
import zprpc.demo.api.DemoService;

@Component
@Service(id = "serviceProducer", interfaceValue = "zprpc.demo.api.DemoService")
public class ServiceProducer implements DemoService {
    @Override
    public String sayHello(String name) {
        return "hello " + name;
    }
}

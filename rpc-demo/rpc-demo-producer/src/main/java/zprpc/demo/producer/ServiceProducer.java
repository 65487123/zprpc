package zprpc.demo.producer;

import com.lzp.annotation.Service;
import zprpc.demo.api.DemoService;

@Service(id = "serviceProducer", interfaceValue = "zprpc.demo.api.DemoService")
public class ServiceProducer implements DemoService {
    @Override
    public String sayHello(String name) {
        return "hello " + name;
    }
}

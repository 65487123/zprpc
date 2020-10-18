package zprpc.demo.producer;

import com.lzp.annotation.Service;
import zprpc.demo.api.DemoService;


@Service(id = "blokingServiceProducer", interfaceValue = "zprpc.demo.api.DemoService")
public class BlokingServiceProducer implements DemoService {
    @Override
    public String sayHello(String name) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "hello " + name;
    }
}

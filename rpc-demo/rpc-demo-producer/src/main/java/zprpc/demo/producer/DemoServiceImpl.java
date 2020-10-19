package zprpc.demo.producer;


import com.lzp.annotation.Service;
import org.springframework.stereotype.Component;
import zprpc.demo.api.DemoService;

/**
 * @author zeping lu
 */
@Component
@Service(id = "demoService", interfaceValue = "zprpc.demo.api.DemoService")
public class DemoServiceImpl implements DemoService {

    @Override
    public String sayHello(String name) {
        return "hello " + name;
    }

}

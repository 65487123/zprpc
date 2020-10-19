package zprpc.demo.producer;

import com.lzp.annotation.Service;
import zprpc.demo.api.DemoService;
import zprpc.demo.api.TakeSecondService;


/**
 * @author zeping lu
 */
@Service(id = "takeSecondService", interfaceValue = "zprpc.demo.api.TakeSecondService")
public class TakeSecondServiceImpl implements TakeSecondService {
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

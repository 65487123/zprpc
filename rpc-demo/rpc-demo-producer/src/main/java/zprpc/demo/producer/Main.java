package zprpc.demo.producer;

import com.lzp.netty.Server;
import com.lzp.util.SpringUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import java.util.concurrent.CountDownLatch;


/**
 * @author zeping lu
 */
@SpringBootApplication
//把SpringUtil这个类导入到spring容器中，这样在初始化服务的时候，如果spring容器中有实例，就会用spring容器中的实例
@Import(SpringUtil.class)
public class Main {
    public static void main(String[] args) throws InterruptedException {
        ///如果需要用到spring，就加入下面这行，这里不需要
        //SpringApplication.run(Main.class, args);
        Server.startRpcServer(5555);
        new CountDownLatch(1).await();
    }
}

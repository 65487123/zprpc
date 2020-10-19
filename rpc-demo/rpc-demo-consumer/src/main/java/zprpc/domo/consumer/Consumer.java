package zprpc.domo.consumer;

import com.alibaba.nacos.api.exception.NacosException;
import com.lzp.ServiceFactory;
import com.lzp.util.ThreadFactoryImpl;
import zprpc.demo.api.DemoService;
import zprpc.demo.api.TakeSecondService;

import java.util.concurrent.*;

/**
 * @author zeping lu
 */
public class Consumer {
    public static void main(String[] args) throws NacosException, InterruptedException {
        //得到远程代理对象
        DemoService demoService = (DemoService) ServiceFactory.getServiceBean("demoService", DemoService.class);
        //发起rpc调用并输出结果
        System.out.println(demoService.sayHello("zeping"));
        System.out.println(demoService.sayHello("zeping"));

        //单线程调用测性能
        long now = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            demoService.sayHello("zeping");
        }
        System.out.println(System.currentTimeMillis() - now);

        //多线程rpc调用测试性能
        ExecutorService executorService = new ThreadPoolExecutor(20, 20, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryImpl("rpc call"));
        now = System.currentTimeMillis();
        CountDownLatch countDownLatch = new CountDownLatch(100000);
        for (int i = 0; i < 100000; i++) {
            executorService.execute(() -> {
                demoService.sayHello("zeping");
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        System.out.println(System.currentTimeMillis() - now);

        //获取带超时事件的rpc代理对象
        TakeSecondService demoService1 = (TakeSecondService) ServiceFactory.getServiceBean("takeSecondService", TakeSecondService.class, 1);
        //超时没返回会抛出异常
        demoService1.sayHello("zeping");
    }
}

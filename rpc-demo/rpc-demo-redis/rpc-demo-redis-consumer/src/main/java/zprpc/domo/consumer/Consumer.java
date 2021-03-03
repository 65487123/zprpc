 /* Copyright zeping lu
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */

package zprpc.domo.consumer;

 import com.alibaba.nacos.api.exception.NacosException;
 import com.lzp.zprpc.client.redis.ServiceFactory;
 import com.lzp.zprpc.common.util.ThreadFactoryImpl;
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

         //单线程调用测性能。 第一次会慢点，因为server端需要初始化线程池中的线程
         long now = System.currentTimeMillis();
         for (int i = 0; i < 10000; i++) {
             demoService.sayHello("zeping");
         }
         System.out.println(System.currentTimeMillis() - now);

         //多线程rpc调用测试性能
         //服务端线程池数量设置得比逻辑cpu要多，因为实际场景service很有可能会进行io操作。而这里没有，所以这里的测试出的并不是最好结果
         ExecutorService executorService = new ThreadPoolExecutor(25, 25, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryImpl("rpc call"));
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

         //获取带超时时间的rpc代理对象
         TakeSecondService demoService1 = (TakeSecondService) ServiceFactory.getServiceBean("takeSecondService", TakeSecondService.class, 1);

         //超时没返回会抛出异常
         demoService1.sayHello("zeping");

     }
 }
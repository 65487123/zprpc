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

package zprpc.demo.nacos.producer;

 import com.lzp.zprpc.server.nacos.netty.Server;
 import org.springframework.boot.autoconfigure.SpringBootApplication;


 /**
 * @author zeping lu
 *
 * 打成jar包独立启动测试结果更准确
 */
@SpringBootApplication
///把SpringUtil这个类导入到spring容器中，这样在初始化服务的时候，如果spring容器中有实例，就会用spring容器中的实例
//@Import(SpringUtil.class)
public class Main {
    public static void main(String[] args) throws Exception {
        ///如果需要用到spring，就加入下面这行，这里不需要
        //SpringApplication.run(Main.class, args);
        Server.startRpcServer();
        //测试手动关闭服务
        /*Thread.sleep(10000);
        Server.closeRpcServer(0, TimeUnit.SECONDS);
        Thread.sleep(3000);
        Server.startRpcServer();*/
    }
}

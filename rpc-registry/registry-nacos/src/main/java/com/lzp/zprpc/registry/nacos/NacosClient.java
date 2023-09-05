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

 package com.lzp.zprpc.registry.nacos;

 import com.alibaba.nacos.api.exception.NacosException;
 import com.alibaba.nacos.api.naming.NamingFactory;
 import com.alibaba.nacos.api.naming.NamingService;
 import com.lzp.zprpc.common.annotation.Service;
 import com.lzp.zprpc.common.util.SpringUtil;
 import com.lzp.zprpc.registry.common.api.RegistryClient;
 import com.lzp.zprpc.registry.common.util.ClazzUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.util.HashMap;
 import java.util.Map;
 import java.util.Set;

 /**
  * Description:实现了统一接口的nacos客户端
  *
  * @author: Lu ZePing
  * @date: 2020/10/9 14:10
  */
 public class NacosClient implements RegistryClient {
     private static final Logger LOGGER = LoggerFactory.getLogger(NacosClient.class);

     NamingService namingService;

     {
         try {
             namingService = NamingFactory.createNamingService(HOST);
         } catch (NacosException e) {
             LOGGER.error("init nameservice failed", e);
         }
     }


     /**
      * 扫描指定包下所有类，获得所有被com.lzp.zprpc.common.annotation.@Service修饰的类，返回实例（如果项目用到了Spring，就到
      * Spring容器中找，找不到才自己初始化一个),并注册到注册中心中
      * <p>
      * ______________________________________________
      * |               namespace                     |
      * |   ————————————————————————————————————————  |
      * |  | ____________ group____________________ | |
      * |  || |------------service--------------| | | |
      * |  || | |cluser |          | cluster|   | | | |
      * |  || | |_______|          |________|   | | | |
      * |  || |_________________________________| | | |
      * |  ||_____________________________________| | |
      * |  |_______________________________________ | |
      * ———————————————————————————————————————————————
      * group和serviceid决定一个服务，一个service包含多个cluster，每个cluster
      * 里包含多个instance
      *
      * @param basePack 要扫描的包
      * @param ip       要注册进注册中心的实例（instance)ip
      * @param port     要注册进注册中心的实例（instance)port
      */

     @Override
     public Map<String, Object> searchAndRegiInstance(String basePack, String ip, int port) throws NacosException, InstantiationException, IllegalAccessException, ClassNotFoundException {
         Map<String, Object> idServiceMap = new HashMap(16);
         for (String path : ClazzUtils.getClazzName(basePack)) {
             regiInstanceIfNecessary(ip, port, idServiceMap, Class.forName(path));
         }
         return idServiceMap;
     }

     @Override
     public void deregiServices(Set<String> serivces, String ip, int port) throws Exception {
         for (String service : serivces) {
             namingService.deregisterInstance(service, ip, port);
         }
     }


     private void regiInstanceIfNecessary(String ip, int port, Map<String, Object> idServiceMap, Class cls) throws InstantiationException, IllegalAccessException, NacosException {
         if (cls.isAnnotationPresent(Service.class)) {
             String id = getId((Service) cls.getAnnotation(Service.class));
             Map<String, Object> nameInstanceMap = SpringUtil.getBeansOfType(cls);
             if (nameInstanceMap.size() != 0) {
                 idServiceMap.put(id, nameInstanceMap.entrySet().iterator().next().getValue());
             } else {
                 idServiceMap.put(id, cls.newInstance());
             }
             regiInstUntilSuccess(id, ip, port);
         }
     }

     /**
     *
     *注册服务时,如果nacos不可用导致注册抛异常,进行重试,直到注册成功
     * */
     private void regiInstUntilSuccess(String serviceName, String ip, int port) {
         while (true) {
             try {
                 namingService.registerInstance(serviceName, ip, port);
             } catch (NacosException e) {
                 try {
                     LOGGER.error("service registration failure",e);
                     Thread.sleep(5000);
                     LOGGER.info("start registering again");
                 } catch (InterruptedException ignored) {
                 }
                 continue;
             }
             break;
         }
     }



     private String getId(Service service) {
         String name;
         String group;
         if ((name = service.name()).startsWith("$")) {
             name = System.getenv(name.substring(1));
         }
         if ((group = service.group()).startsWith("$")) {
             group = System.getenv(group.substring(1));
         }
         return name + "." + group;
     }

     @Override
     public void close() throws Exception {
         namingService.shutDown();
     }


 }

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
import com.lzp.zprpc.registry.api.RegistryClient;
import com.lzp.zprpc.registry.util.ClazzUtils;
import com.lzp.zprpc.common.util.PropertyUtil;
import com.lzp.zprpc.common.util.SpringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

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
         String nacosIpFromEnv;
         try {
             namingService = NamingFactory.createNamingService((nacosIpFromEnv = System
                     .getenv("rpc_registry")) == null ? PropertyUtil.getNacosIpList() : nacosIpFromEnv);
         } catch (NacosException e) {
             LOGGER.error("init nameservice failed", e);
         }
     }

     @Override
     public Map<String, Object> searchAndRegiInstance(String basePack, String ip, int port) throws NacosException, InstantiationException, IllegalAccessException, ClassNotFoundException {
         return searchAndRegiInstance(basePack,namingService , ip, port);
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
      * @param basePack      要扫描的包
      * @param namingService 注册中心
      * @param ip            要注册进注册中心的实例（instance)ip
      * @param port          要注册进注册中心的实例（instance)port
      */

     private Map<String, Object> searchAndRegiInstance(String basePack, NamingService namingService, String ip, int port) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NacosException {
         Map<String, Object> idServiceMap = new HashMap(16);
         for (String path : ClazzUtils.getClazzName(basePack)) {
             regiInstance(namingService, ip, port, idServiceMap, Class.forName(path));
         }
         return idServiceMap;
     }

     private void regiInstance(NamingService namingService, String ip, int port, Map<String, Object> idServiceMap, Class cls) throws InstantiationException, IllegalAccessException, NacosException {
         if (cls.isAnnotationPresent(Service.class)) {
             Service service = (Service) cls.getAnnotation(Service.class);
             Map<String, Object> nameInstanceMap = SpringUtil.getBeansOfType(cls);
             if (nameInstanceMap.size() != 0) {
                 idServiceMap.put(service.id(), nameInstanceMap.entrySet().iterator().next().getValue());
             } else {
                 idServiceMap.put(service.id(), cls.newInstance());
             }
             namingService.registerInstance(service.id(), ip, port);
         }
     }

     @Override
     public void close() throws Exception {
         namingService.shutDown();
     }
 }

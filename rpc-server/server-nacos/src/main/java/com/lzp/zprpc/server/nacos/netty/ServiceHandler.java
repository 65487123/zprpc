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

 package com.lzp.zprpc.server.nacos.netty;

 import com.lzp.zprpc.common.constant.Cons;
 import com.lzp.zprpc.common.dtos.RequestDTO;
 import com.lzp.zprpc.common.dtos.ResponseDTO;
 import com.lzp.zprpc.common.util.OptimizedArrBlockQueue;
 import com.lzp.zprpc.common.util.PropertyUtil;
 import com.lzp.zprpc.common.util.SearialUtil;
 import com.lzp.zprpc.common.util.ThreadFactoryImpl;
 import com.lzp.zprpc.registry.common.api.RegistryClient;
 import com.lzp.zprpc.registry.nacos.NacosClient;
 import com.lzp.zprpc.server.common.util.LogoUtil;
 import io.netty.channel.ChannelHandlerContext;
 import io.netty.channel.SimpleChannelInboundHandler;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.lang.reflect.Method;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.ThreadPoolExecutor;
 import java.util.concurrent.TimeUnit;

 /**
  * Description:根据消息调用相应服务的handler
  *
  * @author: Lu ZePing
  * @date: 2020/9/29 21:31
  */
 public class ServiceHandler extends SimpleChannelInboundHandler<byte[]> {
     private static final Logger LOGGER = LoggerFactory.getLogger(ServiceHandler.class);

     private static Map<String, Object> idServiceMap;

     private static ExecutorService serviceThreadPool;

     @Override
     protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) {
         serviceThreadPool.execute(() -> {
             RequestDTO requestDTO = (RequestDTO) SearialUtil.deserialize(bytes);
             try {
                 Object service = idServiceMap.get(requestDTO.getServiceId());
                 Method method = service.getClass().getMethod(requestDTO.getMethodName(), requestDTO.getParamTypes());
                 channelHandlerContext.writeAndFlush(SearialUtil.serialize(new ResponseDTO(method
                         .invoke(service, requestDTO.getParams()), requestDTO.getThreadId())));
             } catch (Exception e) {
                 channelHandlerContext.writeAndFlush(SearialUtil.serialize(new ResponseDTO(Cons.EXCEPTION + getDetailMsgOfException(e), requestDTO.getThreadId())));
             }
         });
     }

     static void initServiceThreadPool() {
         int logicalCpuCore = Runtime.getRuntime().availableProcessors();
         //被调用的服务可能会涉及到io操作，所以核心线程数设置比逻辑处理器个数多点
         serviceThreadPool = new ThreadPoolExecutor(logicalCpuCore + 1, 2 * logicalCpuCore,
                 100, TimeUnit.SECONDS, new OptimizedArrBlockQueue<>(100000),
                 new ThreadFactoryImpl("rpc service"), (r, executor) -> r.run());
     }

     static boolean shutDownServiceThreadPool(long timeToWait, TimeUnit unit) throws InterruptedException {
         serviceThreadPool.shutdown();
         return serviceThreadPool.awaitTermination(timeToWait, unit);
     }


     /**
      * @return 注册中心客户端
      */
     static RegistryClient regiService() {
         try {
             //默认用nacos做注册中心
             RegistryClient registryClient = new NacosClient();
             idServiceMap = registryClient.searchAndRegiInstance(PropertyUtil.getBasePack(), Server.getIp(), Server.getPort());
             LogoUtil.printLogo();
             LOGGER.info("publish service successfully");
             return registryClient;
         } catch (Exception e) {
             LOGGER.error(e.getMessage(), e);
             return null;
         }
     }

     private String getDetailMsgOfException(Throwable t) {
         Throwable th;
         do {
             th = t;
         } while ((t = t.getCause()) != null);
         return th.getMessage();
     }

     static Set<String> getRegisteredServices() {
         return idServiceMap.keySet();
     }
 }

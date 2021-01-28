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

package com.lzp.netty;

import com.lzp.constant.Cons;
import com.lzp.dtos.RequestDTO;
import com.lzp.dtos.ResponseDTO;
import com.lzp.registry.nacos.NacosClient;
import com.lzp.util.PropertyUtil;
import com.lzp.util.RequestSearialUtil;
import com.lzp.util.ResponseSearialUtil;
import com.lzp.util.ThreadFactoryImpl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.LogoUtil;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Description:根据消息调用相应服务的handler
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 21:31
 */
public class ServiceHandler extends SimpleChannelInboundHandler<byte[]> {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHandler.class);

    private static Map<String, Object> idServiceMap;

    private static ExecutorService serviceThreadPool;

    static {
        int logicalCpuCore = Runtime.getRuntime().availableProcessors();
        //被调用的服务可能会涉及到io操作，所以核心线程数设置比逻辑处理器个数多点
        serviceThreadPool = new ThreadPoolExecutor(logicalCpuCore + 1, 2 * logicalCpuCore,
                100, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100000),
                new ThreadFactoryImpl("rpc service"), (r, executor) -> r.run());
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) {
        serviceThreadPool.execute(() -> {
            RequestDTO requestDTO = RequestSearialUtil.deserialize(bytes);
            try {
                channelHandlerContext.writeAndFlush(ResponseSearialUtil.serialize(new ResponseDTO(requestDTO.getMethod()
                        .invoke(idServiceMap.get(requestDTO.getServiceId()), requestDTO.getPrams()), requestDTO.getThreadId())));
            } catch (Exception e) {
                channelHandlerContext.writeAndFlush(ResponseSearialUtil.serialize(new ResponseDTO(Cons.EXCEPTION + e.getMessage(), requestDTO.getThreadId())));
            }
        });
    }


    static void rigiService() {
        try {
            //默认用nacos做注册中心
            //暂时也只实现了用nacos做注册中心，如果后续有时间可以加入其他注册中心实现，那么就需要配置文件中加配置，然后这里读取配置，选择new具体的注册中心
            /*
                RegistryClient registryClient;
                switch(配置文件读出的注册中心配置){
                    case "xxx":registryClient = xxxClient();
                    break;
                    ...
                    ...
                }
            */
            idServiceMap = new NacosClient().searchAndRegiInstance(PropertyUtil.getBasePack(), Server.getIp(), Server.getPort());
            LogoUtil.printLogo();
            logger.info("publish service successfully");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    static void rigiService(ClassLoader classLoader) {
        try {
            //默认用nacos做注册中心
            //暂时也只实现了用nacos做注册中心，如果后续有时间可以加入其他注册中心实现，那么就需要配置文件中加配置，然后这里读取配置，选择new具体的注册中心
            /*
                RegistryClient registryClient;
                switch(配置文件读出的注册中心配置){
                    case "xxx":registryClient = xxxClient();
                    break;
                    ...
                    ...
                }
            */
            idServiceMap = new NacosClient().searchAndRegiInstance(PropertyUtil.getBasePack(classLoader), Server.getIp(), Server.getPort(),classLoader);
            LogoUtil.printLogo();
            logger.info("publish service successfully");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}

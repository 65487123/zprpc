package com.lzp.netty;

import com.lzp.dtos.RequestDTO;
import com.lzp.dtos.ResponseDTO;
import com.lzp.registry.nacos.NacosClient;
import com.lzp.util.PropertyUtil;
import com.lzp.util.RequestSearialUtil;
import com.lzp.util.ResponseSearialUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Description:根据消息调用相应服务的handler
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 21:31
 */
public class ServiceHandler extends SimpleChannelInboundHandler<byte[]> {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHandler.class);

    private static Map<String, Object> idServiceMap;


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) {
        RequestDTO requestDTO = RequestSearialUtil.deserialize(bytes);
        try {
            channelHandlerContext.writeAndFlush(ResponseSearialUtil.serialize(new ResponseDTO(requestDTO.getMethod()
                    .invoke(idServiceMap.get(requestDTO.getServiceId()), requestDTO.getPrams()), requestDTO.getThreadId())));
        } catch (Exception e) {
            channelHandlerContext.writeAndFlush(ResponseSearialUtil.serialize(new ResponseDTO("exceptionÈ" + e.getMessage(), requestDTO.getThreadId())));
        }
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
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}

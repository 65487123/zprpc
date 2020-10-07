package com.lzp.netty;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.lzp.dtos.RequestDTO;
import com.lzp.dtos.ResponseDTO;
import com.lzp.util.RegisterUtil;
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
    private static NamingService namingService ;

    static {
        try {
            namingService = NamingFactory.createNamingService(PropertyUtil.getNacosIpList());
            idServiceMap = RegisterUtil.searchAndRegiInstance(PropertyUtil.getBasePack(), namingService, Server.ip, Server.port);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes)  {
        RequestDTO requestDTO = RequestSearialUtil.deserialize(bytes);

        try {
            channelHandlerContext.writeAndFlush(ResponseSearialUtil.serialize(new ResponseDTO(requestDTO.getMethod()
                    .invoke(idServiceMap.get(requestDTO.getServiceId()), requestDTO.getPrams()))));
        } catch (Exception e){
            channelHandlerContext.writeAndFlush(ResponseSearialUtil.serialize(new ResponseDTO("exceptionÈ"+e.getMessage())));
        }
    }

}

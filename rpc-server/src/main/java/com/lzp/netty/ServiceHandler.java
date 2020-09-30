package com.lzp.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Description:根据消息调用相应服务的handler
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 21:31
 */
public class ServiceHandler extends SimpleChannelInboundHandler<byte[]> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) throws Exception {

    }
}

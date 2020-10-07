package com.lzp.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Description:处理服务端返回的RPC调用结果
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 21:31
 */
public class ResultHandler extends SimpleChannelInboundHandler<byte[]> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) throws Exception {

    }
}

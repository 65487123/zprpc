package com.lzp.zprpc.client.netty;

import com.lzp.zprpc.common.zpproto.LzpMessageDecoder;
import com.lzp.zprpc.common.zpproto.LzpMessageEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @Author：luzeping
 * @Date: 2019/1/6 20:39
 */

public class SocketChannelInitializer extends ChannelInitializer {
    @Override
    protected void initChannel(Channel channel) {
        channel.pipeline().addLast(new IdleStateHandler(90, 30, 0))
                .addLast(new LzpMessageDecoder(false)).addLast(new LzpMessageEncoder())
                .addLast("resultHandler", new ResultHandler());
    }
}

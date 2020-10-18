package com.lzp.netty;

import com.lzp.zpproto.LzpMessageDecoder;
import com.lzp.zpproto.LzpMessageEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @Author：luzeping
 * @Date: 2019/1/6 20:39
 */

public class SocketChannelInitializerForServer extends ChannelInitializer {
    @Override
    protected void initChannel(Channel channel) {
        channel.pipeline().addLast(new IdleStateHandler(15,Integer.MAX_VALUE,Integer.MAX_VALUE))
                .addLast(new LzpMessageDecoder()).addLast(new LzpMessageEncoder())
                .addLast("serviceHandler",new ServiceHandler());
    }
}

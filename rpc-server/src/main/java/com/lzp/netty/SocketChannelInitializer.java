package com.lzp.netty;

import com.lzp.protocol.zpproto.LzpMessageDecoder;
import com.lzp.protocol.zpproto.LzpMessageEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.stereotype.Service;

/**
 * @Author：luzeping
 * @Date: 2019/1/6 20:39
 */
@Service
public class SocketChannelInitializer extends ChannelInitializer {
    @Override
    protected void initChannel(Channel channel) {
        channel.pipeline().addLast(new IdleStateHandler(150,Integer.MAX_VALUE,Integer.MAX_VALUE))
                .addLast(new LzpMessageDecoder()).addLast(new LzpMessageEncoder())
                .addLast("serviceHandler",new ServiceHandler());
    }
}

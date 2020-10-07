package com.lzp.zpproto;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LzpMessageDecoder extends ReplayingDecoder<Void> {
    private static final Logger logger = LoggerFactory.getLogger(LzpMessageDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        int length = byteBuf.readInt();
        if (length == 0){
            return;
        }
        byte[] content = new byte[length];
        byteBuf.readBytes(content);
        list.add(content);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();
        }
    }

}

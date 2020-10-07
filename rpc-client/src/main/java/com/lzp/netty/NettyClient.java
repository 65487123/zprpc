package com.lzp.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description:nettyclient
 *
 * @author: Lu ZePing
 * @date: 2020/9/27 18:32
 */
public class NettyClient implements AutoCloseable{

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);
    public static EventLoopGroup workerGroup = new NioEventLoopGroup(1);
    private static Bootstrap bootstrap = new Bootstrap();

    static {
        bootstrap.group(workerGroup).channel(NioSocketChannel.class).handler(new SocketChannelInitializer());
    }

    
    public static Channel getChannel(String ip, int port) throws InterruptedException {
        return bootstrap.bind(ip, port).sync().channel();

    }

    @Override
    public void close() throws Exception {
        workerGroup.shutdownGracefully();
    }
}

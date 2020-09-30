package com.lzp.netty;

import com.lzp.util.PropertyUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description:nettyserver
 *
 * @author: Lu ZePing
 * @date: 2020/9/27 18:32
 */
public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);


    public static void startRpcServer(){
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        EventLoopGroup workerGroup = new NioEventLoopGroup(2);
        serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new SocketChannelInitializer());
        try {
            serverBootstrap.bind(PropertyUtil.getPort()).sync();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

package com.lzp.zprpc.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description:nettyclient
 *
 * @author: Lu ZePing
 * @date: 2020/9/27 18:32
 */
public class NettyClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyClient.class);
    public static EventLoopGroup workerGroup = new NioEventLoopGroup(1);
    private static Bootstrap bootstrap = new Bootstrap();

    static {
        bootstrap.group(workerGroup)
                ///测了下，禁用Nagle算法并没有带来明显的性能提升，考虑到会占用更多带宽，暂时就不开启
                /*.option(ChannelOption.TCP_NODELAY, true)*/
                .channel(NioSocketChannel.class).handler(new SocketChannelInitializer());
    }


    public static Channel getChannel(String ip, int port) {
        try {
            return bootstrap.connect(ip, port).sync().channel();
        } catch (InterruptedException e) {
            return getChannel(ip, port);
        }
    }

    @Override
    public void close() {
        workerGroup.shutdownGracefully();
    }
}

 /* Copyright zeping lu
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */

package com.lzp.zprpc.server.netty;

import com.lzp.zprpc.common.constant.Cons;
import com.lzp.zprpc.common.exception.NoFreeIpException;
import com.lzp.zprpc.common.exception.NoFreePortException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Enumeration;

/**
 * Description:nettyserver
 *
 * @author: Lu ZePing
 * @date: 2020/9/27 18:32
 */
public class Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private static String ip;
    private static int port;
    private static EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private static EventLoopGroup workerGroup = new NioEventLoopGroup(1);

    public synchronized static void startRpcServer(String ip, int port) {
        startServer0(ip, port);
        ServiceHandler.rigiService();
    }

    public static void startRpcServer(int port) {
        startRpcServer(null, port);
    }

    public static void startRpcServer() {
        startRpcServer(null, 0);
    }


    public synchronized static void startRpcServer(String ip, int port,ClassLoader classLoader) {
        startServer0(ip,port);
        ServiceHandler.rigiService(classLoader);
    }

    public static void startRpcServer(int port,ClassLoader classLoader) {
        startRpcServer(null, port,classLoader);
    }

    public static void startRpcServer(ClassLoader classLoader) {
        startRpcServer(null, 0,classLoader);
    }
    public static void closeServer() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public static String getIp() {
        return ip;
    }

    public static int getPort() {
        return port;
    }

    public static String getIpAddress(String excludedIp) {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();
                if (!netInterface.isLoopback() && !netInterface.isVirtual() && netInterface.isUp() && !netInterface.getDisplayName().contains(Cons.DOCKER_NAME)) {
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        ip = addresses.nextElement();
                        String address;
                        if (ip instanceof Inet4Address && !excludedIp.equals(address = ip.getHostAddress())) {
                            return address;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("failed to find ip", e);
        }
        throw new NoFreeIpException("All ip ports are occupied");
    }

    private static synchronized void startServer0(String ip, int port){
        if (Server.port != 0) {
            throw new RuntimeException("The server has started");
        }
        Server.ip = ip == null ? getIpAddress("") : ip;
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                ///测了下，禁用Nagle算法并没有带来明显的性能提升，考虑到会占用更多带宽，暂时就不开启
                /*.childOption(ChannelOption.TCP_NODELAY,true)*/
                .childHandler(new SocketChannelInitializerForServer());
        try {
            Channel channel;
            if (port == 0) {
                for (; ; ) {
                    try {
                        channel = bind(Server.ip, serverBootstrap);
                        break;
                    } catch (NoFreePortException e) {
                        //进到这里说明,上一个ip的端口已经被占用完了，如果是指定ip的,直接抛异常
                        if (ip == null) {
                            Server.ip = getIpAddress(Server.ip);
                        } else {
                            throw e;
                        }
                    }
                }
            } else {
                channel = serverBootstrap.bind(Server.ip, Server.port = port).sync().channel();
            }
            channel.closeFuture().addListener((GenericFutureListener<ChannelFuture>) future -> Server.closeServer());
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static Channel bind(String ip, ServerBootstrap serverBootstrap) {
        Channel channel;
        for (int i = Cons.MIN_PORT; i < Cons.MAX_PORT; i++) {
            try {
                channel = serverBootstrap.bind(ip, i).sync().channel();
                Server.port = i;
                return channel;
            } catch (Exception ignored) {
            }
        }
        throw new NoFreePortException("No free port");
    }
}

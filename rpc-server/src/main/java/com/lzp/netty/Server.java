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

package com.lzp.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
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
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static String ip;
    private static int port;
    private static EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private static EventLoopGroup workerGroup = new NioEventLoopGroup(1);

    public static void startRpcServer(String ip, int port) {
        if (Server.port != 0) {
            throw new RuntimeException("The server has started");
        }
        Server.ip = ip == null ? getIpAddress() : ip;
        Server.port = port;
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                ///测了下，禁用Nagle算法并没有带来明显的性能提升，考虑到会占用更多带宽，暂时就不开启
                /*.childOption(ChannelOption.TCP_NODELAY,true)*/
                .childHandler(new SocketChannelInitializerForServer());
        try {
            serverBootstrap.bind(Server.ip, port).sync();
            ServiceHandler.rigiService();
            logger.info("publish service successfully");
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void startRpcServer(int port) {
        startRpcServer(null, port);
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

    public static String getIpAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();
                if (!netInterface.isLoopback() && !netInterface.isVirtual() && netInterface.isUp() && !netInterface.getDisplayName().contains("docker")) {
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        ip = addresses.nextElement();
                        if (ip instanceof Inet4Address) {
                            return ip.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("failed to find ip", e);
        }
        return null;
    }

}

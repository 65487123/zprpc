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
     private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
     private static String ip;
     private static int port;
     private static EventLoopGroup bossGroup;
     private static EventLoopGroup workerGroup;

     public synchronized static void startRpcServer(String ip, int port) {
         if (Server.port != 0) {
             throw new RuntimeException("The server has started");
         }
         bossGroup = new NioEventLoopGroup(1);
         workerGroup = new NioEventLoopGroup(1);
         startServer0(ip, port);
         ServiceHandler.rigiService();
     }

     public static void startRpcServer(int port) {
         startRpcServer(null, port);
     }

     public static void startRpcServer() {
         startRpcServer(null, 0);
     }

     /**
      * 关闭server(释放端口),如果端口已经释放或者server根本没起则不会做任何操作。
      * <p>
      * server关闭后(释放端口),一般注册中心是检查不到到服务实例不健康的并移除实例的,
      * 需要关闭注册中心的客户端才能检测到(前提是注册的实例ip和这个客户端主机的ip一致)。
      * <p>
      * 但是有些注册中心没有提供关闭客户端的api(比如nacos)。
      * 如果用到这样的注册中心,需要在关闭服务(但不停JVM)时手动移除服务.(当需要关闭server时,提供的服务bean肯定是要
      * 被销毁了,一般销毁前肯会掉销毁方法(destroy-method),只要在这个方法里把服务实例从注册中心中移除就行。
      *
      * @return 关闭服务操作是否成功执行
      */
     public static boolean closeServer() {
         if (Server.port != 0) {
             bossGroup.shutdownGracefully();
             workerGroup.shutdownGracefully();
             bossGroup = null;
             workerGroup = null;
             Server.port = 0;
             LOGGER.info("Service stopped successfully");
             return true;
         }
         return false;
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
                 if (!netInterface.isLoopback() && !netInterface.isVirtual() && netInterface.isUp()
                         && !netInterface.getDisplayName().contains(Cons.DOCKER_NAME)) {
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

     private static synchronized void startServer0(String ip, int port) {
         Server.ip = ip == null ? getIpAddress("") : ip;
         ServerBootstrap serverBootstrap = new ServerBootstrap()
                 .group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
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
             channel.closeFuture().addListener(future -> Server.closeServer());
         } catch (InterruptedException e) {
             LOGGER.error(e.getMessage(), e);
         }
     }


     private static Channel bind(String ip, ServerBootstrap serverBootstrap) {
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

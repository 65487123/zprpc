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
 import com.lzp.zprpc.common.exception.NoFreePortException;
 import com.lzp.zprpc.registry.api.RegistryClient;
 import io.netty.bootstrap.ServerBootstrap;
 import io.netty.channel.Channel;
 import io.netty.channel.EventLoopGroup;
 import io.netty.channel.nio.NioEventLoopGroup;
 import io.netty.channel.socket.nio.NioServerSocketChannel;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.net.Socket;
 import java.util.concurrent.TimeUnit;

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
     /**
      * Description:
      * 在这保留注册中心的客户端的引用是为了实现关闭server的时候关闭注册中心客户端
      * <p>
      * 因为在不停JVM的情况下,server关闭后(释放端口),注册中心一般是检测不到到这个服务实例不健康并移除的,
      * 需要关闭注册中心的客户端才能检测到(前提是注册的实例ip和这个客户端主机的ip一致)。
      */
     private static RegistryClient registryClient;

     public synchronized static void startRpcServer(String ip, int port) {
         if (Server.port != 0) {
             throw new RuntimeException("The server has started");
         }
         bossGroup = new NioEventLoopGroup(1);
         workerGroup = new NioEventLoopGroup(1);
         startServer0(ip, port);
         ServiceHandler.initServiceThreadPool();
         registryClient = ServiceHandler.regiService();
     }

     public static void startRpcServer(int port) {
         startRpcServer(null, port);
     }

     public static void startRpcServer() {
         startRpcServer(null, 0);
     }

     /**
      * 关闭server(释放端口),如果端口已经释放或者server根本没起则不会做任何操作。
      *
      * @return 关闭服务操作是否成功执行
      */
     public synchronized static boolean closeRpcServer(long timeToWait, TimeUnit unit) throws Exception {
         if (Server.port != 0) {
             registryClient.deregiServices(ServiceHandler.getRegisteredServices(), ip, port);
             ServiceHandler.shutDownServiceThreadPool(timeToWait, unit);
             bossGroup.shutdownGracefully();
             workerGroup.shutdownGracefully();
             bossGroup = null;
             workerGroup = null;
             Server.port = 0;
             if (registryClient != null) {
                 registryClient.close();
                 registryClient = null;
             }
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


     ///不去遍历网卡了，用能连同注册中心的ip来发布
     /*public static String getIpAddress(List<String> excludedIp) {
         try {
             Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
             InetAddress ip;
             String ifName;
             while (allNetInterfaces.hasMoreElements()) {
                 NetworkInterface netInterface = allNetInterfaces.nextElement();
                 if (!netInterface.isLoopback() && !netInterface.isVirtual() && netInterface.isUp()
                         && !(ifName = netInterface.getDisplayName()).contains(Cons.DOCKER_NAME)
                         && !ifName.contains(Cons.K8S_NAME)) {
                     Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                     while (addresses.hasMoreElements()) {
                         ip = addresses.nextElement();
                         String address;
                         if (ip instanceof Inet4Address && !excludedIp.contains(address = ip.getHostAddress())) {
                             return address;
                         }
                     }
                 }
             }
         } catch (Exception e) {
             LOGGER.error("failed to find ip", e);
         }
         throw new NoFreeIpException("All ip ports are occupied");
     }*/

     /*private static synchronized void startServer0(String ip, int port) {
         List<String> excludedIps= new ArrayList<>();
         Server.ip = ip == null ? getIpAddress(null) : ip;
         ServerBootstrap serverBootstrap = new ServerBootstrap()
                 .group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                 ///测了下，禁用Nagle算法并没有带来明显的性能提升，考虑到会占用更多带宽，暂时就不开启
                 *//*.childOption(ChannelOption.TCP_NODELAY,true)*//*
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
                             excludedIps.add(ip);
                             Server.ip = getIpAddress(excludedIps);
                         } else {
                             throw e;
                         }
                     }
                 }
             } else {
                 channel = serverBootstrap.bind(Server.ip, Server.port = port).sync().channel();
             }
             channel.closeFuture().addListener(future -> Server.closeRpcServer(0, TimeUnit.SECONDS));
         } catch (InterruptedException e) {
             LOGGER.error(e.getMessage(), e);
         }
     }*/

     /**
     *找到能和注册中心建立TCP连接的ip
     * 如果连不上注册中心，返回环回地址
      * */
     private static String getLocalIpAddressToRegistry() {
         Socket socket = null;
         try {
             String[] hostAndPort = RegistryClient.HOST.split(Cons.COMMA)[0].split(":");
             socket = new Socket(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
             return socket.getLocalAddress().getHostAddress();
         } catch (Exception e) {
             return "127.0.0.1";
         } finally {
             try {
                 assert socket != null;
                 socket.close();
             } catch (Exception ignored) {
             }
         }
     }


     private static synchronized void startServer0(String ip, int port) {
         Server.ip = ip == null ? getLocalIpAddressToRegistry() : ip;
         ServerBootstrap serverBootstrap = new ServerBootstrap()
                 .group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                 ///测了下，禁用Nagle算法并没有带来明显的性能提升，考虑到会占用更多带宽，暂时就不开启
                 /*.childOption(ChannelOption.TCP_NODELAY,true)*/
                 .childHandler(new SocketChannelInitializerForServer());
         try {
             Channel channel;
             if (port == 0) {
                 channel = bind(Server.ip, serverBootstrap);
             } else {
                 channel = serverBootstrap.bind(Server.ip, Server.port = port).sync().channel();
             }
             channel.closeFuture().addListener(future -> Server.closeRpcServer(0, TimeUnit.SECONDS));
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

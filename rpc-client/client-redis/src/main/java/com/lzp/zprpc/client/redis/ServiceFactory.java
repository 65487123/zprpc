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

 package com.lzp.zprpc.client.redis;


 import com.lzp.zprpc.client.connectionpool.FixedShareableChannelPool;
 import com.lzp.zprpc.client.connectionpool.ServiceChannelPoolImp;
 import com.lzp.zprpc.client.connectionpool.SingleChannelPool;
 import com.lzp.zprpc.client.netty.ResultHandler;
 import com.lzp.zprpc.common.constant.Cons;
 import com.lzp.zprpc.common.dtos.RequestDTO;
 import com.lzp.zprpc.common.exception.CallException;
 import com.lzp.zprpc.common.exception.RemoteException;
 import com.lzp.zprpc.common.exception.RpcTimeoutException;
 import com.lzp.zprpc.common.util.PropertyUtil;
 import com.lzp.zprpc.common.util.SearialUtil;
 import com.lzp.zprpc.common.util.ThreadFactoryImpl;
 import com.lzp.zprpc.registry.common.api.RegistryClient;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.lang.reflect.Method;
 import java.lang.reflect.Proxy;
 import java.net.ConnectException;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.*;
 import java.util.concurrent.locks.LockSupport;

 /**
  * Description:提供代理bean，用以远程调服务。代理bean是单例的
  * <p>
  * 用redis做注册中心,目前还不是非常完善。当被注册进redis的服务,如果一段时间其主机网卡出问题了,
  * 这时消费者发现这个服务不可用,会把他从redis中移除。过了一段时间,出问题的主机网络恢复了,但是不会
  * 再次把自己注册进redis一次,导致消费者访问不到这个服务。所以,如果出现这个问题,只能重启。
  * <p>
  * 虽说这个问题如果发生也只是浪费了点资源，并且出现的概率比较低,出现了也能通过重启解决。
  * 但是也是可以从代码上解决的,可以从redis客户端上着手,和redis建完连接就监听这个连接(如果
  * java客户端没监听api,可以自己开线程异步轮询,判断连接状态),如果连接被动关闭了就重连,
  * 重连上以后把自己原先注册过的服务再注册一次就行(注册服务是幂等操作,就算重复注册也无所谓)。
  *
  * @author: Lu ZePing
  * @date: 2020/9/27 18:32
  */
 public class ServiceFactory {
     private static final Logger LOGGER = LoggerFactory.getLogger(ServiceFactory.class);

     private static Map<String, BeanAndAllHostAndPort> serviceIdInstanceMap = new ConcurrentHashMap<>();
     private static FixedShareableChannelPool channelPool;
     private static ExecutorService refreshServiceThreadPool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryImpl("refreshService"));
     private static PooledRedisClient redisClient;

     static {
         try {
             String connectionPoolSize;
             if ((connectionPoolSize = PropertyUtil.getConnetionPoolSize()) == null) {
                 channelPool = new SingleChannelPool();
             } else {
                 channelPool = new ServiceChannelPoolImp(Integer.parseInt(connectionPoolSize));
             }
             redisClient = new PooledRedisClient(new RedisClientPool(5, RegistryClient.HOST));
         } catch (Exception e) {
             LOGGER.error("Throw an exception when initializing refis client", e);
         }
         refreshServiceThreadPool.execute(() -> {
             for (; ; ) {
                 try {
                     Thread.sleep(20000);
                 } catch (InterruptedException e) {
                     LOGGER.error(e.getMessage(), e);
                 }
                 for (Map.Entry<String, BeanAndAllHostAndPort> entry : serviceIdInstanceMap.entrySet()) {
                     entry.getValue().hostAndPorts = redisClient.getAndTransformToList(entry.getKey());
                 }
             }
         });
     }


     private static final class BeanAndAllHostAndPort {
         private volatile Object bean;
         private volatile List<String> hostAndPorts;
         private volatile Object beanWithTimeOut;

         public BeanAndAllHostAndPort(Object bean, List<String> hostAndPorts, Object beanWithTimeOut) {
             this.bean = bean;
             this.hostAndPorts = hostAndPorts;
             this.beanWithTimeOut = beanWithTimeOut;
         }
     }

     public static <T> T getServiceBean(String serviceName, Class<T> interfaceCls) {
         return (T) getServiceBean(serviceName, "default", interfaceCls);
     }

     /**
      * Description:获取远程服务代理对象，通过这个对象可以调用远程服务的方法，就和调用本地方法一样
      * 代理对象是单例的
      *
      * @param serviceName  需要远程调用的服务名
      * @param group        需要远程调用的组
      * @param interfaceCls 本地和远程服务实现的接口
      */
     public static Object getServiceBean(String serviceName, String group, Class interfaceCls) {
         String serviceId = serviceName + "." + group;
         BeanAndAllHostAndPort beanAndAllHostAndPort;
         if ((beanAndAllHostAndPort = serviceIdInstanceMap.get(serviceId)) == null) {
             synchronized (ServiceFactory.class) {
                 if (serviceIdInstanceMap.get(serviceId) == null) {
                     List<String> hostAndPorts = new CopyOnWriteArrayList<>(redisClient.getAndTransformToList(serviceId));
                     serviceIdInstanceMap.put(serviceId, beanAndAllHostAndPort = new BeanAndAllHostAndPort(null, hostAndPorts, null));
                     Object bean = getServiceBean0(serviceId, interfaceCls);
                     beanAndAllHostAndPort.bean = bean;
                     return bean;
                 } else {
                     return serviceIdInstanceMap.get(serviceId).bean;
                 }
             }
         } else {
             if (beanAndAllHostAndPort.bean == null) {
                 synchronized (ServiceFactory.class) {
                     if (beanAndAllHostAndPort.bean == null) {
                         beanAndAllHostAndPort.bean = getServiceBean0(serviceId, interfaceCls);
                     }
                     return beanAndAllHostAndPort.bean;
                 }
             } else {
                 return beanAndAllHostAndPort.bean;
             }
         }
     }

     public static <T> T getServiceBean(String serviceName, Class<T> interfaceCls, int timeout) {
         return (T) getServiceBean(serviceName, "default", interfaceCls, timeout);
     }

     /**
      * Description:获取远程服务代理对象，通过这个对象可以调用远程服务的方法，就和调用本地方法一样
      * 代理对象是单例的
      *
      * @param serviceName  需要远程调用的服务名
      * @param group        需要远程调用的组
      * @param interfaceCls 本地和远程服务实现的接口
      * @param timeout      rpc调用的超时时间,单位是毫秒,超过这个时间没返回则抛 {@link RpcTimeoutException}
      */
     public static Object getServiceBean(String serviceName, String group, Class interfaceCls, int timeout) {
         checkTimeOut(timeout);
         String serviceId = serviceName + "." + group;
         BeanAndAllHostAndPort beanAndAllHostAndPort;
         if ((beanAndAllHostAndPort = serviceIdInstanceMap.get(serviceId)) == null) {
             synchronized (ServiceFactory.class) {
                 if (serviceIdInstanceMap.get(serviceId) == null) {
                     List<String> hostAndPorts = new CopyOnWriteArrayList<>(redisClient.getAndTransformToList(serviceId));
                     serviceIdInstanceMap.put(serviceId, beanAndAllHostAndPort = new BeanAndAllHostAndPort(null, hostAndPorts, null));
                     Object beanWithTimeOut = getServiceBean0(serviceId, interfaceCls, timeout);
                     beanAndAllHostAndPort.beanWithTimeOut = beanWithTimeOut;
                     return beanWithTimeOut;
                 } else {
                     return serviceIdInstanceMap.get(serviceId).beanWithTimeOut;
                 }
             }
         } else {
             if (beanAndAllHostAndPort.beanWithTimeOut == null) {
                 synchronized (ServiceFactory.class) {
                     if (beanAndAllHostAndPort.beanWithTimeOut == null) {
                         beanAndAllHostAndPort.beanWithTimeOut = getServiceBean0(serviceId, interfaceCls, timeout);
                     }
                     return beanAndAllHostAndPort.beanWithTimeOut;
                 }
             } else {
                 return beanAndAllHostAndPort.beanWithTimeOut;
             }
         }
     }

      /* public static Object getAsyServiceBean(String serviceId, Class interfaceCls, int timeout) {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{interfaceCls}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                return null;
            }
        });
    }*/


     /**
      * Description:校验参数
      **/
     private static void checkTimeOut(int timeout) {
         if (timeout <= 0) {
             throw new IllegalArgumentException("timeout need to be greater than 0");
         }
     }


     private static Object getServiceBean0(String serviceId, Class interfaceCls) {
         BeanAndAllHostAndPort beanAndAllHostAndPort = serviceIdInstanceMap.get(serviceId);
         return Proxy.newProxyInstance(ServiceFactory.class.getClassLoader(),
                 new Class[]{interfaceCls}, (proxy, method, args) -> {
                     Object result;
                     if ((result = callAndGetResult(method, serviceId, beanAndAllHostAndPort, Long.MAX_VALUE, args)) instanceof String &&
                             ((String) result).startsWith(Cons.EXCEPTION)) {
                         throw new RemoteException(((String) result).substring(Cons.THREE));
                     }
                     return result;
                 });
     }

     private static Object getServiceBean0(String serviceId, Class interfaceCls, int timeout) {
         BeanAndAllHostAndPort beanAndAllHostAndPort = serviceIdInstanceMap.get(serviceId);
         return Proxy.newProxyInstance(ServiceFactory.class.getClassLoader(),
                 new Class[]{interfaceCls}, (proxy, method, args) -> {
                     Object result = callAndGetResult(method, serviceId, beanAndAllHostAndPort, System.currentTimeMillis() + timeout, args);
                     if (result instanceof String && ((String) result).startsWith(Cons.EXCEPTION)) {
                         String message;
                         if (Cons.TIMEOUT.equals(message = ((String) result).substring(Cons.THREE))) {
                             throw new RpcTimeoutException("rpc timeout");
                         } else {
                             throw new RemoteException(message);
                         }
                     }
                     return result;
                 });
     }

     private static Object callAndGetResult(Method method, String serviceId, BeanAndAllHostAndPort beanAndAllHostAndPort,
                                            long deadline, Object... args) throws Exception {
         String ipAndport = "";
         List<String> hostAndPorts = beanAndAllHostAndPort.hostAndPorts;
         try {
             //根据serviceid找到所有提供这个服务的ip+port
             Thread thisThread = Thread.currentThread();
             ResultHandler.ThreadResultAndTime threadResultAndTime = new ResultHandler.ThreadResultAndTime(deadline, thisThread);
             ResultHandler.reqIdThreadMap.put(thisThread.getId(), threadResultAndTime);
             channelPool.getChannel(ipAndport = hostAndPorts.get(ThreadLocalRandom.current().nextInt(hostAndPorts.size())))
                     .writeAndFlush(SearialUtil.serialize(new RequestDTO(thisThread.getId(), serviceId, method.getName(), method.getParameterTypes(), args)));
             Object result;
             //用while，防止虚假唤醒
             while ((result = threadResultAndTime.getResult()) == null) {
                 LockSupport.park();
             }
             return result;
         } catch (ConnectException e) {
             hostAndPorts.remove(ipAndport);
             redisClient.sremove(serviceId, ipAndport);
             if (System.currentTimeMillis() > deadline) {
                 ResultHandler.reqIdThreadMap.remove(Thread.currentThread().getId());
                 return Cons.EXCEPTION + Cons.TIMEOUT;
             } else {
                 return callAndGetResult(method, serviceId, beanAndAllHostAndPort, deadline, args);
             }
         } catch (IllegalArgumentException e) {
             ResultHandler.reqIdThreadMap.remove(Thread.currentThread().getId());
             throw new CallException("no service available");
         }
     }

 }

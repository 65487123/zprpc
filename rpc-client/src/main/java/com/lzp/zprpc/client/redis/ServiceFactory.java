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


 import com.alibaba.nacos.api.exception.NacosException;
 import com.alibaba.nacos.api.naming.NamingService;
 import com.alibaba.nacos.api.naming.listener.Event;
 import com.alibaba.nacos.api.naming.listener.EventListener;
 import com.alibaba.nacos.api.naming.listener.NamingEvent;
 import com.alibaba.nacos.api.naming.pojo.Instance;
 import com.lzp.zprpc.client.connectionpool.FixedShareableChannelPool;
 import com.lzp.zprpc.client.connectionpool.ServiceChannelPoolImp;
 import com.lzp.zprpc.client.connectionpool.SingleChannelPool;
 import com.lzp.zprpc.client.netty.ResultHandler;
 import com.lzp.zprpc.common.constant.Cons;
 import com.lzp.zprpc.common.dtos.RequestDTO;
 import com.lzp.zprpc.common.exception.CallException;
 import com.lzp.zprpc.common.exception.RpcException;
 import com.lzp.zprpc.common.util.PropertyUtil;
 import com.lzp.zprpc.common.util.RequestSearialUtil;
 import com.lzp.zprpc.common.util.ThreadFactoryImpl;
 import com.lzp.zprpc.registry.api.RedisClient;
 import com.lzp.zprpc.registry.util.RedisClientUtil;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.lang.reflect.Method;
 import java.lang.reflect.Proxy;
 import java.net.ConnectException;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.*;
 import java.util.concurrent.locks.LockSupport;

 /**
  * Description:提供代理bean，用以远程调服务。代理bean是单例的
  *
  * @author: Lu ZePing
  * @date: 2020/9/27 18:32
  */
 public class ServiceFactory {
     private static final Logger LOGGER = LoggerFactory.getLogger(ServiceFactory.class);

     private static Map<String, BeanAndAllHostAndPort> serviceIdInstanceMap = new ConcurrentHashMap<>();
     private static RedisClient redisClient;
     private static FixedShareableChannelPool channelPool;
     private static ExecutorService refreshServiceThreadPool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryImpl("refreshService"));
     private static RedisClientPool redisClientPool;
     static {
         try {
             //如果被依赖的包和这个不在同一个classpath下,这段代码就没用了
             String redisIpList;
             redisClient = RedisClientUtil.newRedisClient(redisIpList = PropertyUtil.getProperties().getProperty(Cons.REDIS_IP_LIST));
             String connectionPoolSize;
             if ((connectionPoolSize = PropertyUtil.getConnetionPoolSize()) == null) {
                 channelPool = new SingleChannelPool();
             } else {
                 channelPool = new ServiceChannelPoolImp(Integer.parseInt(connectionPoolSize));
             }
             redisClientPool = new RedisClientPool(5, redisIpList);
         } catch (Exception e) {
             LOGGER.error("Throw an exception when initializing NamingService", e);
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

     /**
      * Description:获取远程服务代理对象，通过这个对象可以调用远程服务的方法，就和调用本地方法一样
      * 代理对象是单例的
      *
      * @param serviceId    需要远程调用的服务id
      * @param interfaceCls 本地和远程服务实现的接口
      */
     public static Object getServiceBean(String serviceId, Class interfaceCls) throws NacosException {
         return getServiceBean0(serviceId, interfaceCls, null);
     }

     /**
      * Description:获取远程服务代理对象，通过这个对象可以调用远程服务的方法，就和调用本地方法一样
      * 代理对象是单例的
      *
      * @param serviceId    需要远程调用的服务id
      * @param interfaceCls 本地和远程服务实现的接口
      * @param timeout      rpc调用的超时时间,单位是毫秒,超过这个时间没返回则抛 {@link TimeoutException}
      */
     public static Object getServiceBean(String serviceId, Class interfaceCls, int timeout) throws NacosException {
         return getServiceBean0(serviceId, interfaceCls, timeout, null);
     }

     /**
      * Description:获取远程服务代理对象，通过这个对象可以调用远程服务的方法，就和调用本地方法一样
      * 代理对象是单例的
      * 这个方法用在jar包不在同一个classpath的情况下
      *
      * @param serviceId    需要远程调用的服务id
      * @param interfaceCls 本地和远程服务实现的接口
      * @param classLoader  加载nacos类的类加载器
      */
     public static Object getServiceBean(String serviceId, Class interfaceCls, ClassLoader classLoader) throws NacosException {
         initialNameServiceAndChannelPool(classLoader);
         return getServiceBean0(serviceId, interfaceCls, classLoader);
     }

     /**
      * Description:获取远程服务代理对象，通过这个对象可以调用远程服务的方法，就和调用本地方法一样
      * 代理对象是单例的
      * 这个方法用在jar包不在同一个classpath的情况下
      *
      * @param serviceId    需要远程调用的服务id
      * @param interfaceCls 本地和远程服务实现的接口
      * @param timeout      rpc调用的超时时间,单位是毫秒，超过这个时间没返回则抛 {@link TimeoutException}
      * @param classLoader  加载nacos类的类加载器
      */
     public static Object getServiceBean(String serviceId, Class interfaceCls, int timeout, ClassLoader classLoader) throws NacosException {
         initialNameServiceAndChannelPool(classLoader);
         return getServiceBean0(serviceId, interfaceCls, timeout, classLoader);
     }


     public static Object getServiceBean0(String serviceId, Class interfaceCls, ClassLoader classLoader) throws NacosException {
         if (serviceIdInstanceMap.get(serviceId) == null) {
             synchronized (ServiceFactory.class) {
                 if (serviceIdInstanceMap.get(serviceId) == null) {
                     List<String> hostAndPorts = new ArrayList<>(redisClient.getAndTransformToList(serviceId));
                     Object bean = getBeanCore(serviceId, interfaceCls, classLoader);
                     serviceIdInstanceMap.put(serviceId, new BeanAndAllHostAndPort(bean, hostAndPorts, null));
                     return bean;
                 } else {
                     return serviceIdInstanceMap.get(serviceId).bean;
                 }
             }
         } else {
             BeanAndAllHostAndPort beanAndAllHostAndPort = serviceIdInstanceMap.get(serviceId);
             if (beanAndAllHostAndPort.bean == null) {
                 synchronized (ServiceFactory.class) {
                     if (serviceIdInstanceMap.get(serviceId).bean == null) {
                         beanAndAllHostAndPort.bean = getBeanCore(serviceId, interfaceCls, classLoader);
                     }
                     return beanAndAllHostAndPort.bean;
                 }
             } else {
                 return beanAndAllHostAndPort.bean;
             }
         }
     }


     public static Object getServiceBean0(String serviceId, Class interfaceCls, int timeout, ClassLoader classLoader) throws NacosException {
         checkTimeOut(timeout);
         if (serviceIdInstanceMap.get(serviceId) == null) {
             synchronized (ServiceFactory.class) {
                 if (serviceIdInstanceMap.get(serviceId) == null) {
                     List<String> hostAndPorts = new ArrayList<>(redisClient.getAndTransformToList(serviceId));
                     Object beanWithTimeOut = getBeanWithTimeOutCore(serviceId, interfaceCls, timeout, classLoader);
                     serviceIdInstanceMap.put(serviceId, new BeanAndAllHostAndPort(null, hostAndPorts, beanWithTimeOut));
                     return beanWithTimeOut;
                 } else {
                     return serviceIdInstanceMap.get(serviceId).beanWithTimeOut;
                 }
             }
         } else {
             BeanAndAllHostAndPort beanAndAllHostAndPort = serviceIdInstanceMap.get(serviceId);
             if (beanAndAllHostAndPort.beanWithTimeOut == null) {
                 synchronized (ServiceFactory.class) {
                     if (serviceIdInstanceMap.get(serviceId).beanWithTimeOut == null) {
                         beanAndAllHostAndPort.beanWithTimeOut = getBeanWithTimeOutCore(serviceId, interfaceCls, timeout, classLoader);
                     }
                     return beanAndAllHostAndPort.beanWithTimeOut;
                 }
             } else {
                 return beanAndAllHostAndPort.beanWithTimeOut;
             }
         }
     }

     /**
      * Description:指定类加载器获取代理类,说明用到这个rpc框架的包和这个包肯定不在同一个classpath下,那么这个类加载时的初始化肯定报错,
      * naming肯定为null,在第一次获取代理类时初始化就行，如果已经初始化过了，就不用再初始化了,因为跑在一个jvm中的类，配置肯定一样.
      * 初始化不加锁是因为初始化操作是幂等操作
      */
     private static void initialNameServiceAndChannelPool(ClassLoader classLoader) throws NacosException {
         if (redisClient == null) {
             String redisIpList;
             redisClient = RedisClientUtil.newRedisClient(redisIpList = PropertyUtil.getProperties(classLoader).getProperty(Cons.REDIS_IP_LIST));
             String connectionPoolSize;
             if ((connectionPoolSize = PropertyUtil.getConnetionPoolSize()) == null) {
                 channelPool = new SingleChannelPool();
             } else {
                 channelPool = new ServiceChannelPoolImp(Integer.parseInt(connectionPoolSize));
             }
             redisClientPool = new RedisClientPool(5, redisIpList);
         }
     }


     /**
      * Description:校验参数
      **/
     private static void checkTimeOut(int timeout) {
         if (timeout <= 0) {
             throw new IllegalArgumentException("timeout need to be greater than 0");
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


     private static Object getBeanCore(String serviceId, Class interfaceCls, ClassLoader classLoader) {
         return Proxy.newProxyInstance(classLoader == null ? ServiceFactory.class.getClassLoader() : classLoader,
                 new Class[]{interfaceCls}, (proxy, method, args) -> {
                     Object result;
                     if ((result = callAndGetResult(method, serviceId, Long.MAX_VALUE, args)) instanceof String &&
                             ((String) result).startsWith(Cons.EXCEPTION)) {
                         throw new RpcException(((String) result).substring(Cons.TEN));
                     }
                     return result;
                 });
     }

     private static Object getBeanWithTimeOutCore(String serviceId, Class interfaceCls, int timeout, ClassLoader classLoader) {
         return Proxy.newProxyInstance(classLoader == null ? ServiceFactory.class.getClassLoader() : classLoader,
                 new Class[]{interfaceCls}, (proxy, method, args) -> {
                     Object result = callAndGetResult(method, serviceId, System.currentTimeMillis() + timeout, args);
                     if (result instanceof String && ((String) result).startsWith(Cons.EXCEPTION)) {
                         String message;
                         if (Cons.TIMEOUT.equals(message = ((String) result).substring(Cons.TEN))) {
                             throw new TimeoutException();
                         } else {
                             throw new RpcException(message);
                         }
                     }
                     return result;
                 });
     }

     private static Object callAndGetResult(Method method, String serviceId, long deadline, Object... args) throws Exception {
         String ipAndport = "";
         try {
             //根据serviceid找到所有提供这个服务的ip+port
             List<String> hostAndPorts = serviceIdInstanceMap.get(serviceId).hostAndPorts;
             Thread thisThread = Thread.currentThread();
             ResultHandler.ThreadResultAndTime threadResultAndTime = new ResultHandler.ThreadResultAndTime(deadline, thisThread);
             ResultHandler.reqIdThreadMap.put(thisThread.getId(), threadResultAndTime);
             channelPool.getChannel(ipAndport = hostAndPorts.get(ThreadLocalRandom.current().nextInt(hostAndPorts.size())))
                     .writeAndFlush(RequestSearialUtil.serialize(new RequestDTO(thisThread.getId(), serviceId, method.getName(), method.getParameterTypes(), args)));
             Object result;
             //用while，防止虚假唤醒
             while ((result = threadResultAndTime.getResult()) == null) {
                 LockSupport.park(thisThread);
             }
             return result;
         } catch (Exception e) {
             if (e instanceof ConnectException) {
                 redisClientPool.getClient().sremove(serviceId, ipAndport);
                 return callAndGetResult(method, serviceId, deadline, args);
             } else if (e instanceof IllegalArgumentException) {
                 e = new CallException("no service available");
             }
             throw e;
         }
     }

 }

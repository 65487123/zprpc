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

package com.lzp.zprpc.client.nacos;


import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.lzp.zprpc.client.connectionpool.FixedShareableChannelPool;
import com.lzp.zprpc.client.connectionpool.ServiceChannelPoolImp;
import com.lzp.zprpc.client.connectionpool.SingleChannelPool;
import com.lzp.zprpc.common.constant.Cons;
import com.lzp.zprpc.common.dtos.RequestDTO;
import com.lzp.zprpc.common.exception.CallException;
import com.lzp.zprpc.common.exception.RemoteException;
import com.lzp.zprpc.client.netty.ResultHandler;
import com.lzp.zprpc.common.exception.RpcTimeoutException;
import com.lzp.zprpc.common.util.PropertyUtil;
import com.lzp.zprpc.common.util.RequestSearialUtil;
import com.lzp.zprpc.registry.api.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
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
     private static NamingService naming;
     private static FixedShareableChannelPool channelPool;

     static {
         try {
             String nacosIpFromEnv;
             //如果需要查配置文件,必须打在同一classpath下(如果是OSGI环境,可以通过插件配置)
             naming = NamingFactory.createNamingService(RegistryClient.HOST);
             String connectionPoolSize;
             if ((connectionPoolSize = PropertyUtil.getConnetionPoolSize()) == null) {
                 channelPool = new SingleChannelPool();
             } else {
                 channelPool = new ServiceChannelPoolImp(Integer.parseInt(connectionPoolSize));
             }
         } catch (NacosException e) {
             LOGGER.error("Throw an exception when initializing NamingService", e);
         }
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


     public static Object getServiceBean(String serviceName, Class interfaceCls) throws NacosException {
         return getServiceBean(serviceName, "default", interfaceCls);
     }

     /**
      * Description:获取远程服务代理对象，通过这个对象可以调用远程服务的方法，就和调用本地方法一样
      * 代理对象是单例的
      *
      * @param serviceName  需要远程调用的服务名
      * @param group        需要远程调用的组
      * @param interfaceCls 本地和远程服务实现的接口
      */
     public static Object getServiceBean(String serviceName, String group, Class interfaceCls) throws NacosException {
         String serviceId = serviceName + "." + group;
         BeanAndAllHostAndPort beanAndAllHostAndPort;
         if ((beanAndAllHostAndPort = serviceIdInstanceMap.get(serviceId)) == null) {
             synchronized (ServiceFactory.class) {
                 if (serviceIdInstanceMap.get(serviceId) == null) {
                     List<String> hostAndPorts = new ArrayList<>();
                     for (Instance instance : naming.selectInstances(serviceId, true)) {
                         hostAndPorts.add(instance.getIp() + Cons.COLON + instance.getPort());
                     }
                     Object bean = getServiceBean0(serviceId, interfaceCls);
                     serviceIdInstanceMap.put(serviceId, new BeanAndAllHostAndPort(bean, hostAndPorts, null));
                     addListener(serviceId);
                     return bean;
                 } else {
                     return serviceIdInstanceMap.get(serviceId).bean;
                 }
             }
         } else {
             if (beanAndAllHostAndPort.bean == null) {
                 synchronized (ServiceFactory.class) {
                     if (serviceIdInstanceMap.get(serviceId).bean == null) {
                         beanAndAllHostAndPort.bean = getServiceBean0(serviceId, interfaceCls);
                     }
                     return beanAndAllHostAndPort.bean;
                 }
             } else {
                 return beanAndAllHostAndPort.bean;
             }
         }
     }

     public static Object getServiceBean(String serviceName, Class interfaceCls, int timeout) throws NacosException {
         return getServiceBean(serviceName, "default", interfaceCls, timeout);
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
     public static Object getServiceBean(String serviceName, String group, Class interfaceCls, int timeout) throws NacosException {
         checkTimeOut(timeout);
         String serviceId = serviceName + "." + group;
         BeanAndAllHostAndPort beanAndAllHostAndPort;
         if ((beanAndAllHostAndPort = serviceIdInstanceMap.get(serviceId)) == null) {
             synchronized (ServiceFactory.class) {
                 if (serviceIdInstanceMap.get(serviceId) == null) {
                     List<String> hostAndPorts = new ArrayList<>();
                     for (Instance instance : naming.selectInstances(serviceId, true)) {
                         hostAndPorts.add(instance.getIp() + Cons.COLON + instance.getPort());
                     }
                     Object beanWithTimeOut = getServiceBean0(serviceId, interfaceCls, timeout);
                     serviceIdInstanceMap.put(serviceId, new BeanAndAllHostAndPort(null, hostAndPorts, beanWithTimeOut));
                     addListener(serviceId);
                     return beanWithTimeOut;
                 } else {
                     return serviceIdInstanceMap.get(serviceId).beanWithTimeOut;
                 }
             }
         } else {
             if (beanAndAllHostAndPort.beanWithTimeOut == null) {
                 synchronized (ServiceFactory.class) {
                     if (serviceIdInstanceMap.get(serviceId).beanWithTimeOut == null) {
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


     /**
      * Description:监听指定服务。当被监听的服务实例列表发生变化，更新本地缓存
      **/
     private static void addListener(String serviceId) throws NacosException {
         naming.subscribe(serviceId, new EventListener() {
             BeanAndAllHostAndPort beanAndAllHostAndPort = serviceIdInstanceMap.get(serviceId);

             @Override
             public void onEvent(Event event) {
                 if (event instanceof NamingEvent) {
                     List<String> newHostAndPorts = new ArrayList<>();
                     for (Instance instance : ((NamingEvent) event).getInstances()) {
                         newHostAndPorts.add(instance.getIp() + Cons.COLON + instance.getPort());
                     }
                     beanAndAllHostAndPort.hostAndPorts = newHostAndPorts;
                 }
             }
         });
     }


     private static Object getServiceBean0(String serviceId, Class interfaceCls) {
         return Proxy.newProxyInstance(ServiceFactory.class.getClassLoader(),
                 new Class[]{interfaceCls}, (proxy, method, args) -> {
                     Object result;
                     if ((result = callAndGetResult(method, serviceId, Long.MAX_VALUE, args)) instanceof String &&
                             ((String) result).startsWith(Cons.EXCEPTION)) {
                         throw new RemoteException(((String) result).substring(Cons.THREE));
                     }
                     return result;
                 });
     }

     private static Object getServiceBean0(String serviceId, Class interfaceCls, int timeout) {
         return Proxy.newProxyInstance(ServiceFactory.class.getClassLoader(),
                 new Class[]{interfaceCls}, (proxy, method, args) -> {
                     Object result = callAndGetResult(method, serviceId, System.currentTimeMillis() + timeout, args);
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

     private static Object callAndGetResult(Method method, String serviceId, long deadline, Object... args) throws Exception {
         try {
             //根据serviceid找到所有提供这个服务的ip+port
             List<String> hostAndPorts = serviceIdInstanceMap.get(serviceId).hostAndPorts;
             Thread thisThread = Thread.currentThread();
             ResultHandler.ThreadResultAndTime threadResultAndTime = new ResultHandler.ThreadResultAndTime(deadline, thisThread);
             ResultHandler.reqIdThreadMap.put(thisThread.getId(), threadResultAndTime);
             channelPool.getChannel(hostAndPorts.get(ThreadLocalRandom.current().nextInt(hostAndPorts.size())))
                     .writeAndFlush(RequestSearialUtil.serialize(new RequestDTO(thisThread.getId(), serviceId, method.getName(), method.getParameterTypes(), args)));
             Object result;
             //用while，防止虚假唤醒
             while ((result = threadResultAndTime.getResult()) == null) {
                 LockSupport.park();
             }
             return result;
         } catch (ConnectException e) {
             //当服务缩容时,服务关闭后,nacos没刷新(nacos如果不是高可用,可能会一直进入这里,直到超时)
             if (System.currentTimeMillis() > deadline) {
                 ResultHandler.reqIdThreadMap.remove(Thread.currentThread().getId());
                 return Cons.EXCEPTION + Cons.TIMEOUT;
             } else {
                 return callAndGetResult(method, serviceId, deadline, args);
             }
         } catch (IllegalArgumentException e) {
             ResultHandler.reqIdThreadMap.remove(Thread.currentThread().getId());
             throw new CallException("no service available");
         }
     }
 }

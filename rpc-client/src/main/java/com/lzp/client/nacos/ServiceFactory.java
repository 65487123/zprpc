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

package com.lzp.client.nacos;


import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.lzp.connectionpool.FixedShareableChannelPool;
import com.lzp.connectionpool.ServiceChannelPoolImp;
import com.lzp.connectionpool.SingleChannelPool;
import com.lzp.constant.Cons;
import com.lzp.dtos.RequestDTO;
import com.lzp.exception.RpcException;
import com.lzp.netty.ResultHandler;
import com.lzp.util.PropertyUtil;
import com.lzp.util.RequestSearialUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

/**
 * Description:提供代理bean，用以远程调服务。代理bean是单例的
 *
 * @author: Lu ZePing
 * @date: 2020/9/27 18:32
 */
public class ServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(ServiceFactory.class);

    private static Map<String, BeanAndAllHostAndPort> serviceIdInstanceMap = new ConcurrentHashMap<>();
    private static NamingService naming;
    private static FixedShareableChannelPool channelPool;
    static {
        try {
            //如果被依赖的包和这个不在同一个classpath下,这段代码就没用了
            naming = NamingFactory.createNamingService(PropertyUtil.getNacosIpList());
            String connectionPoolSize;
            if ((connectionPoolSize = PropertyUtil.getConnetionPoolSize()) == null) {
                channelPool = new SingleChannelPool();
            } else {
                channelPool = new ServiceChannelPoolImp(Integer.parseInt(connectionPoolSize));
            }
        } catch (NacosException e) {
            logger.error("Throw an exception when initializing NamingService", e);
        }
    }

    /**
     * nacos包下的Instance有太多废字段了，实例很多的话会占一定内存，所以用这个类来代替
     */
    public static class HostAndPort {

        private String host;
        private int port;

        public HostAndPort(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof HostAndPort) {
                HostAndPort hp = (HostAndPort) obj;
                return port == hp.port && host.equals(hp.host);
            }
            return false;
        }

        @Override
        public String toString() {
            return host + ":" + port;
        }

        @Override
        public int hashCode() {
            return host.hashCode() + port;
        }
    }

    private static class BeanAndAllHostAndPort {
        private volatile Object bean;
        private volatile List<HostAndPort> hostAndPorts;
        private volatile Object beanWithTimeOut;

        public BeanAndAllHostAndPort(Object bean, List<HostAndPort> hostAndPorts, Object beanWithTimeOut) {
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
    public static Object getServiceBean0(String serviceId, Class interfaceCls, ClassLoader classLoader) throws NacosException {
        if (serviceIdInstanceMap.get(serviceId) == null) {
            synchronized (ServiceFactory.class) {
                if (serviceIdInstanceMap.get(serviceId) == null) {
                    List<HostAndPort> hostAndPorts = new ArrayList<>();
                    for (Instance instance : naming.selectInstances(serviceId, true)) {
                        hostAndPorts.add(new HostAndPort(instance.getIp(), instance.getPort()));
                    }
                    addListener(serviceId);
                    Object bean = getBeanCore(serviceId, interfaceCls,classLoader);
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
                        beanAndAllHostAndPort.bean = getBeanCore(serviceId, interfaceCls,classLoader);
                    }
                    return beanAndAllHostAndPort.bean;
                }
            } else {
                return beanAndAllHostAndPort.bean;
            }
        }
    }


    /**
     * Description:获取远程服务代理对象，通过这个对象可以调用远程服务的方法，就和调用本地方法一样，
     * 增加了超时时间设置，指定时间内没返回结果，则抛出异常.
     * <p>
     * 注意：由于对象是单例保存，只有第一次获取实例设置的超时时间参数是有效的，后面再次获取都会返回第一次生成的对象。
     *
     * @param serviceId    需要远程调用的服务id
     * @param interfaceCls 本地和远程服务实现的接口
     * @param timeout      超时时间，单位是秒
     */
    public static Object getServiceBean0(String serviceId, Class interfaceCls, int timeout, ClassLoader classLoader) throws NacosException {
        checkTimeOut(timeout);
        if (serviceIdInstanceMap.get(serviceId) == null) {
            synchronized (ServiceFactory.class) {
                if (serviceIdInstanceMap.get(serviceId) == null) {
                    List<HostAndPort> hostAndPorts = new ArrayList<>();
                    for (Instance instance : naming.selectInstances(serviceId, true)) {
                        hostAndPorts.add(new HostAndPort(instance.getIp(), instance.getPort()));
                    }
                    addListener(serviceId);
                    Object beanWithTimeOut = getBeanWithTimeOutCore(serviceId, interfaceCls, timeout);
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
                        beanAndAllHostAndPort.beanWithTimeOut = getBeanWithTimeOutCore(serviceId, interfaceCls, timeout);
                    }
                    return beanAndAllHostAndPort.beanWithTimeOut;
                }
            } else {
                return beanAndAllHostAndPort.beanWithTimeOut;
            }
        }
    }

    public static Object getServiceBean(String serviceId, Class interfaceCls, ClassLoader classLoader) throws NacosException {
        initialNameServiceAndChannelPool(classLoader);
        return getServiceBean0(serviceId, interfaceCls, classLoader);
    }


    public static Object getServiceBean(String serviceId, Class interfaceCls, int timeout, ClassLoader classLoader) throws NacosException {
        initialNameServiceAndChannelPool(classLoader);
        return getServiceBean0(serviceId, interfaceCls, timeout, classLoader);
    }

    /**
     * Description:指定类加载器获取代理类,说明用到这个rpc框架的包和这个包肯定不在同一个classpath下,那么这个类加载时的初始化肯定报错,
     * naming肯定为null,在第一次获取代理类时初始化就行，如果已经初始化过了，就不用再初始化了,因为跑在一个jvm中的类，配置肯定一样.
     * 初始化不加锁是因为初始化操作是幂等操作
     */
    private static void initialNameServiceAndChannelPool(ClassLoader classLoader) throws NacosException {
        if (naming == null) {
            naming = createNamingServiceBySpecifiedloader(PropertyUtil.getNacosIpList(classLoader));
            String connectionPoolSize;
            if ((connectionPoolSize = PropertyUtil.getConnetionPoolSize()) == null) {
                channelPool = new SingleChannelPool();
            } else {
                channelPool = new ServiceChannelPoolImp(Integer.parseInt(connectionPoolSize));
            }
        }
    }

    public static Object getServiceBean(String serviceId, Class interfaceCls) throws NacosException {
        return getServiceBean0(serviceId, interfaceCls, null);
    }


    public static Object getServiceBean(String serviceId, Class interfaceCls, int timeout) throws NacosException {
        return getServiceBean0(serviceId, interfaceCls, timeout, null);
    }

    /**
     * Description:如果没有指定类加载器获取代理类，获取代理类的包和这个包应该是同一个classpath下，而基本能判定nacos-client和nacos-api
     * 也在同一个classpath下，所以直接用nacos提供的方法就能加载到(nacos提供的方法是通过反射加载Class,并且不能指定类加载器，默认就是NamingFactory的类加载器)
     * 而如果指定类加载器获取代理类，获取代理类的包和这个包应该不在同一个classpath下,nacos-client和nacos-api也很大可能不在同一个classpath下
     * 所以nacos提供的方法是加载不到的
     */
    private static NamingService createNamingServiceBySpecifiedloader(String serverList) throws NacosException {
        try {
            Class<?> driverImplClass = com.alibaba.nacos.client.naming.NacosNamingService.class;
            Constructor constructor = driverImplClass.getConstructor(String.class);
            NamingService vendorImpl = (NamingService) constructor.newInstance(serverList);
            return vendorImpl;
        } catch (Throwable var4) {
            throw new NacosException(-400, var4);
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

    /**
     * Description:监听指定服务。当被监听的服务实例列表发生变化，更新本地缓存
     *
     * @param serviceId
     * @Date: 2020/10/12 20:16
     * @Return
     **/
    private static void addListener(String serviceId) throws NacosException {
        naming.subscribe(serviceId, new EventListener() {
            BeanAndAllHostAndPort beanAndAllHostAndPort = serviceIdInstanceMap.get(serviceId);

            @Override
            public void onEvent(Event event) {
                if (event instanceof NamingEvent) {
                    List<HostAndPort> newHostAndPorts = new ArrayList<>();
                    for (Instance instance : ((NamingEvent) event).getInstances()) {
                        newHostAndPorts.add(new HostAndPort(instance.getIp(), instance.getPort()));
                    }
                    beanAndAllHostAndPort.hostAndPorts = newHostAndPorts;
                }
            }
        });
    }


    private static Object getBeanCore(String serviceId, Class interfaceCls, ClassLoader classLoader) {
        return Proxy.newProxyInstance(classLoader == null ? ServiceFactory.class.getClassLoader() : classLoader, new Class[]{interfaceCls}, (proxy, method, args) -> {
            //根据serviceid找到所有提供这个服务的ip+port
            List<HostAndPort> hostAndPorts = serviceIdInstanceMap.get(serviceId).hostAndPorts;
            Thread thisThread = Thread.currentThread();
            ResultHandler.ThreadResultAndTime threadResultAndTime = new ResultHandler.ThreadResultAndTime(Long.MAX_VALUE, thisThread);
            ResultHandler.reqIdThreadMap.put(thisThread.getId(), threadResultAndTime);
            channelPool.getChannel(hostAndPorts.get(ThreadLocalRandom.current().nextInt(hostAndPorts.size()))).writeAndFlush(RequestSearialUtil.serialize(new RequestDTO(thisThread.getId(), serviceId, method, args)));
            Object result;
            //用while，防止虚假唤醒
            while ((result = threadResultAndTime.getResult()) == null) {
                LockSupport.park(thisThread);
            }
            return result;
        });
    }

    private static Object getBeanWithTimeOutCore(String serviceId, Class interfaceCls, int timeout) {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{interfaceCls}, (proxy, method, args) -> {
            //根据serviceid找到所有提供这个服务的ip+port
            List<HostAndPort> hostAndPorts = serviceIdInstanceMap.get(serviceId).hostAndPorts;
            Thread thisThread = Thread.currentThread();
            ResultHandler.ThreadResultAndTime threadResultAndTime = new ResultHandler.ThreadResultAndTime(System.currentTimeMillis() + (timeout * 1000), thisThread);
            ResultHandler.reqIdThreadMap.put(thisThread.getId(), threadResultAndTime);
            channelPool.getChannel(hostAndPorts.get(ThreadLocalRandom.current().nextInt(hostAndPorts.size()))).writeAndFlush(RequestSearialUtil.serialize(new RequestDTO(thisThread.getId(), serviceId, method, args)));
            Object result;
            //用while，防止虚假唤醒
            while ((result = threadResultAndTime.getResult()) == null) {
                LockSupport.park(thisThread);
            }
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

}

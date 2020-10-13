package com.lzp;


import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.lzp.util.PropertyUtil;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description:提供代理bean，用以远程调服务。代理bean是单例的
 *
 * @author: Lu ZePing
 * @date: 2020/9/27 18:32
 */
public class ServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(ServiceFactory.class);

    private static Map<String, Channel> serviceIdChannelMap = new ConcurrentHashMap<>();
    private static Map<String, BeanAndAllHostAndPort> serviceIdInstanceMap = new ConcurrentHashMap<>();
    private static NamingService naming;

    static {
        try {
            naming = NamingFactory.createNamingService(PropertyUtil.getNacosIpList());
        } catch (NacosException e) {
            logger.error(e.getMessage(), e);
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
    //System.out.println(naming.selectInstances("nacos.test.3", true));


    /**
     * Description:获取远程服务代理对象，通过这个对象可以调用远程服务的方法，就和调用本地方法一样
     * 代理对象是单例的
     * @param serviceId    需要远程调用的服务id
     * @param interfaceCls 本地和远程服务实现的接口
     */
    public static Object getServiceBean(String serviceId, Class interfaceCls) throws NacosException {
        if (serviceIdInstanceMap.get(serviceId) == null) {
            synchronized (ServiceFactory.class) {
                if (serviceIdInstanceMap.get(serviceId) == null) {
                    List<HostAndPort> hostAndPorts = new ArrayList<>();
                    for (Instance instance : naming.selectInstances(serviceId, true)) {
                        hostAndPorts.add(new HostAndPort(instance.getIp(), instance.getPort()));
                    }
                    addListener(serviceId);
                    Object bean = getBeanCore(serviceId, interfaceCls);
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
                        beanAndAllHostAndPort.bean = getBeanCore(serviceId, interfaceCls);
                    }
                    return beanAndAllHostAndPort.bean;
                }
            } else {
                return beanAndAllHostAndPort.bean;
            }
        }
    }


    /**
     * Description:获取远程服务代理对象，通过这个对象可以调用远程服务的方法，就和调用本地方法一样，增加了超时时间设置，指定时间内没返回结果，则抛出异常
     *
     * @param serviceId  需要远程调用的服务id
     * @param interfaceCls 本地和远程服务实现的接口
     * @param timeout 超时时间，单位是秒
     */
    public static Object getServiceBean(String serviceId, Class interfaceCls, int timeout) {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{interfaceCls}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                return null;
            }
        });
    }

   /* public static Object getAstServiceBean(String serviceId, Class interfaceCls, int timeout) {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{interfaceCls}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                return null;
            }
        });
    }*/

    /**Description:监听指定服务。当被监听的服务实例列表发生变化，更新本地缓存
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


    private static Object getBeanCore(String serviceId, Class interfaceCls){
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{interfaceCls}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //根据serviceid找到所有提供这个服务的ip+port

                return null;
            }
        });
    }
}

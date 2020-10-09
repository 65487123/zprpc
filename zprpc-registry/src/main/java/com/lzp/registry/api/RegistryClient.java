package com.lzp.registry.api;


import com.alibaba.nacos.api.exception.NacosException;

import java.util.Map;

/**
 * Description:注册中心客户端统一接口
 *
 * @author: Lu ZePing
 * @date: 2020/9/27 18:32
 */
public interface RegistryClient {


    /**
     * 扫描指定包下所有类，获得所有被com.lzp.com.lzp.annotation.@Service修饰的类，返回实例。如果项目用到了Spring，就到
     *   Spring容器中找，找不到才自己初始化一个,并注册到注册中心中
     *
     * @param basePack  要扫描的包
     * @param ip  要注册进注册中心的实例（instance)ip
     * @param port  要注册进注册中心的实例（instance)port
     * @return key是服务的唯一id，value是实例
     */
    Map<String, Object> searchAndRegiInstance(String basePack,String ip,int port) throws Exception;




}

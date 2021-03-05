
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

package com.lzp.zprpc.registry.redis;

import com.alibaba.nacos.api.exception.NacosException;
import com.lzp.zprpc.common.annotation.Service;
import com.lzp.zprpc.common.constant.Cons;
import com.lzp.zprpc.common.util.PropertyUtil;
import com.lzp.zprpc.common.util.SpringUtil;
import com.lzp.zprpc.registry.api.RegistryClient;
import com.lzp.zprpc.registry.util.ClazzUtils;
import com.lzp.zprpc.registry.util.RedisClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Description:实现了统一接口的redis客户端
 *
 * @author: Zeping Lu
 * @date: 2021/3/2 14:52
 */
public class RedisClient implements RegistryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisClient.class);

    @Override
    public Map<String, Object> searchAndRegiInstance(String basePack, String ip, int port) throws NacosException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        String redisIpFromEnv;
        return searchAndRegiInstance(basePack, RedisClientUtil.newRedisClient((redisIpFromEnv = System.getenv("rpc_registry")) == null ? PropertyUtil.getProperties().getProperty(Cons.REDIS_IP_LIST) : redisIpFromEnv), ip, port);
    }

    @Override
    public Map<String, Object> searchAndRegiInstance(String basePack, String ip, int port, ClassLoader classLoader) throws Exception {
        String redisIpFromEnv;
        return searchAndRegiInstance(basePack, RedisClientUtil.newRedisClient((redisIpFromEnv = System.getenv("rpc_registry")) == null ? PropertyUtil.getProperties(classLoader).getProperty(Cons.REDIS_IP_LIST) : redisIpFromEnv), ip, port, classLoader);
    }



    /**
     * 扫描指定包下所有类，获得所有被com.lzp.com.lzp.zprpc.common.annotation.@Service修饰的类，返回实例（如果项目用到了Spring，就到
     * Spring容器中找，找不到才自己初始化一个),并注册到redis中
     * <p>
     *
     * @param basePack      要扫描的包
     * @param redisClient   redis客户端
     * @param ip            要注册进注册中心的实例（instance)ip
     * @param port          要注册进注册中心的实例（instance)port
     */


    private Map<String, Object> searchAndRegiInstance(String basePack, com.lzp.zprpc.registry.api.RedisClient redisClient, String ip, int port) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NacosException {
        Map<String, Object> idServiceMap = new HashMap(16);
        try {
            for (String path : ClazzUtils.getClazzName(basePack)) {
                regiInstance(redisClient, ip, port, idServiceMap, Class.forName(path));
            }
        } finally {
            try {
                redisClient.close();
            } catch (Exception e) {
                LOGGER.error("close redisClient failed", e);
            }
        }
        return idServiceMap;
    }

    private Map<String, Object> searchAndRegiInstance(String basePack, com.lzp.zprpc.registry.api.RedisClient redisClient, String ip, int port, ClassLoader classLoader) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Map<String, Object> idServiceMap = new HashMap(16);
        try {
            for (String path : ClazzUtils.getClazzName(basePack, classLoader)) {
                regiInstance(redisClient, ip, port, idServiceMap, Class.forName(path, true, classLoader));
            }
        } finally {
            try {
                redisClient.close();
            } catch (Exception e) {
                LOGGER.error("close redisClient failed", e);
            }
        }
        return idServiceMap;
    }

    private void regiInstance(com.lzp.zprpc.registry.api.RedisClient redisClient, String ip, int port, Map<String, Object> idServiceMap, Class cls) throws InstantiationException, IllegalAccessException {
        if (cls.isAnnotationPresent(Service.class)) {
            Service service = (Service) cls.getAnnotation(Service.class);
            Map<String, Object> nameInstanceMap = SpringUtil.getBeansOfType(cls);
            if (nameInstanceMap.size() != 0) {
                idServiceMap.put(service.id(), nameInstanceMap.entrySet().iterator().next().getValue());
            } else {
                idServiceMap.put(service.id(), cls.newInstance());
            }
            redisClient.sAdd(service.id(), ip + Cons.COLON + port);
        }
    }

}

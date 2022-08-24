
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
import com.lzp.zprpc.common.util.SpringUtil;
import com.lzp.zprpc.registry.api.RegistryClient;
import com.lzp.zprpc.registry.util.ClazzUtils;
import com.lzp.zprpc.registry.util.RedisClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Description:实现了统一接口的redis客户端
 *
 * @author: Zeping Lu
 * @date: 2021/3/2 14:52
 */
public class RedisClient implements RegistryClient {

    com.lzp.zprpc.registry.api.RedisClient redisClient = RedisClientFactory.newRedisClient(HOST);

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisClient.class);


    /**
     * 扫描指定包下所有类，获得所有被com.lzp.zprpc.common.annotation.@Service修饰的类，返回实例（如果项目用到了Spring，就到
     * Spring容器中找，找不到才自己初始化一个),并注册到redis中
     * <p>
     *
     * @param basePack 要扫描的包
     * @param ip       要注册进注册中心的实例（instance)ip
     * @param port     要注册进注册中心的实例（instance)port
     */
    public Map<String, Object> searchAndRegiInstance(String basePack, String ip, int port) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NacosException {
        Map<String, Object> idServiceMap = new HashMap(16);
        try {
            for (String path : ClazzUtils.getClazzName(basePack)) {
                regiInstanceIfNecessary(redisClient, ip, port, idServiceMap, Class.forName(path));
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

    @Override
    public void deregiServices(Set<String> serivces, String ip, int port) throws Exception {
        for (String service : serivces) {
            redisClient.sremove(service, ip + port);
        }
    }


    private void regiInstanceIfNecessary(com.lzp.zprpc.registry.api.RedisClient redisClient, String ip, int port, Map<String, Object> idServiceMap, Class cls) throws InstantiationException, IllegalAccessException {
        if (cls.isAnnotationPresent(Service.class)) {
            String id = getId((Service) cls.getAnnotation(Service.class));
            Map<String, Object> nameInstanceMap = SpringUtil.getBeansOfType(cls);
            if (nameInstanceMap.size() != 0) {
                idServiceMap.put(id, nameInstanceMap.entrySet().iterator().next().getValue());
            } else {
                idServiceMap.put(id, cls.newInstance());
            }
            redisClient.sAdd(id, ip + Cons.COLON + port);
        }
    }

    private String getId(Service service) {
        String name;
        String group;
        if ((name = service.name()).startsWith("$")) {
            name = System.getenv(name.substring(1));
        }
        if ((group = service.group()).startsWith("$")) {
            group = System.getenv(group.substring(1));
        }
        return name + "." + group;
    }

    @Override
    public void close() throws Exception {
        redisClient.close();
    }
}


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

package com.lzp.zprpc.registry.util;


import com.lzp.zprpc.common.constant.Cons;
import com.lzp.zprpc.common.util.StringUtil;
import com.lzp.zprpc.registry.api.RedisClient;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Description:获取redisclient工具类
 *
 * @author: Zeping Lu
 * @date: 2021/3/2 15:35
 */
public class RedisClientFactory {


    public static RedisClient newRedisClient(String redisInstaces) {
        String[] ips;
        if ((ips = redisInstaces.split(Cons.COMMA)).length > 1) {
            Set<HostAndPort> set = new HashSet<>();
            for (String ip : ips) {
                String[] ipAndPort = StringUtil.stringSplit(ip, Cons.COLON);
                set.add(new HostAndPort(ipAndPort[0], Integer.parseInt(ipAndPort[1])));
            }
            JedisCluster jedisCluster = new JedisCluster(set);
            return newJedisClusterWrapper(jedisCluster);
        } else {
            String[] ipAndPort = StringUtil.stringSplit(ips[0], Cons.COLON);
            Jedis jedis = new Jedis(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
            return newJedisWrapper(jedis);
        }
    }

    private static RedisClient newJedisClusterWrapper(JedisCluster jedisCluster){
        return new RedisClient() {

            @Override
            public Long sAdd(String var1, String... var2) {
                return jedisCluster.sadd(var1, var2);
            }

            @Override
            public List<String> getAndTransformToList(String key) {
                return new ArrayList<>(jedisCluster.smembers(key));
            }

            @Override
            public void sremove(String key, String value) {
                jedisCluster.srem(key, value);
            }

            @Override
            public void close() throws Exception {
                jedisCluster.close();
            }
        };
    }

    private static RedisClient newJedisWrapper(Jedis jedis) {
        return new RedisClient() {
            @Override
            public Long sAdd(String var1, String... var2) {
                return jedis.sadd(var1, var2);
            }

            @Override
            public List<String> getAndTransformToList(String key) {
                return new ArrayList<>(jedis.smembers(key));
            }

            @Override
            public void sremove(String key, String value) {
                jedis.srem(key, value);
            }

            @Override
            public void close() throws Exception {
                jedis.close();
            }
        };
    }
}

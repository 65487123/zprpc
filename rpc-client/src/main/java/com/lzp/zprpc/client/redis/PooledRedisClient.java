package com.lzp.zprpc.client.redis;

import com.lzp.zprpc.registry.api.RedisClient;

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

import java.util.List;

public class PooledRedisClient implements RedisClient {

    RedisClientPool redisClientPool;


    public PooledRedisClient(RedisClientPool initializedRedisClientPool) {
        this.redisClientPool = initializedRedisClientPool;
    }

    @Override
    public Long sAdd(String var1, String... var2) {
        RedisClient redisClient = redisClientPool.getClient();
        Long result = redisClientPool.getClient().sAdd(var1, var2);
        redisClientPool.returnClient(redisClient);
        return result;
    }

    @Override
    public List<String> getAndTransformToList(String key) {
        RedisClient redisClient = redisClientPool.getClient();
        List<String> result = redisClient.getAndTransformToList(key);
        redisClientPool.returnClient(redisClient);
        return result;
    }

    @Override
    public void sremove(String key, String Value) {
        RedisClient redisClient = redisClientPool.getClient();
        redisClient.sremove(key, Value);
        redisClientPool.returnClient(redisClient);
    }

    @Override
    public void close() throws Exception {
        redisClientPool.close();
    }
}

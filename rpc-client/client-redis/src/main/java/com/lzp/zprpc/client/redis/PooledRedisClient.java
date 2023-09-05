package com.lzp.zprpc.client.redis;

import com.lzp.zprpc.registry.redis.api.RedisClient;

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

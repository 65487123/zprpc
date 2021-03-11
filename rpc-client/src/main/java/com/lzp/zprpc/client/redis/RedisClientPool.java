
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

import com.lzp.zprpc.registry.api.RedisClient;
import com.lzp.zprpc.registry.util.RedisClientFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Description:redisClient 连接池
 *
 * @author: Zeping Lu
 * @date: 2021/3/3 15:14
 */
public class RedisClientPool implements AutoCloseable{
    List<RedisClient> redisClientList;

    public RedisClientPool(int num,String redisIpList){
        redisClientList = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            redisClientList.add(RedisClientFactory.newRedisClient(redisIpList));
        }
    }

    @Override
    public void close() throws Exception {
        for (RedisClient redisClient : redisClientList) {
            redisClient.close();
        }
    }

    public synchronized RedisClient getClient()  {
        while (redisClientList.size() == 0) {
            try {
                this.wait();
            } catch (InterruptedException ignored){
            }
        }
        return redisClientList.remove(0);
    }

    public synchronized void returnClient(RedisClient redisClient) {
        redisClientList.add(redisClient);
        if (redisClientList.size() == 1) {
            this.notify();
        }
    }
}

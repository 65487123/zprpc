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

package com.lzp.zprpc.client.connectionpool;

import com.lzp.zprpc.client.netty.ConnectionFactory;
import com.lzp.zprpc.common.constant.Cons;
import com.lzp.zprpc.common.util.ThreadFactoryImpl;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Description:单连接的连接池
 *
 * @author: Zeping Lu
 * @date: 2020/10/13 16:34
 */
public class SingleChannelPool implements FixedShareableChannelPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleChannelPool.class);
    private ThreadPoolExecutor heartBeatThreadPool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryImpl("heartBeat"));
    private Map<String, Channel> hostAndPortChannelsMap = new ConcurrentHashMap<>();

    {
        heartBeatThreadPool.execute(this::hearBeat);
    }

    @Override
    public Channel getChannel(String hostAndPort) {
        Channel channel = hostAndPortChannelsMap.get(hostAndPort);
        if (channel == null) {
            synchronized (this) {
                if ((channel = hostAndPortChannelsMap.get(hostAndPort)) == null) {
                    channel = ConnectionFactory.newChannel(hostAndPort.split(Cons.COLON)[0], Integer.parseInt(hostAndPort.split(Cons.COLON)[1]));
                    channel.closeFuture().addListener(future -> hostAndPortChannelsMap.remove(hostAndPort));
                    hostAndPortChannelsMap.put(hostAndPort, channel);
                }
                return channel;
            }
        } else {
            return channel;
        }
    }


    /**
     * Description ：每12秒发送一个心跳包
     **/
    private void hearBeat() {
        while (true) {
            byte[] emptyPackage = new byte[0];
            for (Map.Entry<?, Channel> entry : hostAndPortChannelsMap.entrySet()) {
                entry.getValue().writeAndFlush(emptyPackage);
            }
            try {
                Thread.sleep(12000);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}

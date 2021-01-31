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

package com.lzp.connectionpool;

import com.lzp.client.nacos.ServiceFactory;
import com.lzp.netty.NettyClient;
import com.lzp.util.ThreadFactoryImpl;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Description:单线程的线程池
 *
 * @author: Zeping Lu
 * @date: 2020/10/13 16:34
 */
public class SingleChannelPool implements FixedShareableChannelPool {
    private static final Logger logger = LoggerFactory.getLogger(SingleChannelPool.class);
    private ThreadPoolExecutor heartBeatThreadPool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryImpl("heartBeat"));
    private Map<ServiceFactory.HostAndPort, Channel> hostAndPortChannelsMap = new ConcurrentHashMap<>();

    {
        heartBeatThreadPool.execute(this::hearBeat);
    }

    @Override
    public Channel getChannel(ServiceFactory.HostAndPort hostAndPort) throws InterruptedException {
        Channel channel = hostAndPortChannelsMap.get(hostAndPort);
        if (channel == null) {
            synchronized (this) {
                if ((channel = hostAndPortChannelsMap.get(hostAndPort)) == null) {
                    channel = NettyClient.getChannel(hostAndPort.getHost(), hostAndPort.getPort());
                    updateChannelWhenClosed(hostAndPort, channel);
                    hostAndPortChannelsMap.put(hostAndPort, channel);
                }
                return channel;
            }
        } else {
            return channel;
        }
    }

    private void updateChannelWhenClosed(ServiceFactory.HostAndPort hostAndPort, Channel channel) {
        channel.closeFuture().addListener(future -> {
            /*因为getChannel()会调用ChannelFuture.sync()方法，会阻塞当前线程，不能在io线程中执行下面的代码块。而事件回调却在io线程中执行的
            所以下面这段代码需要在另一个线程中执行。每次都new新线程池是因为连接不可用是小概率事件，线程一直存在会比较耗资源。*/
            ExecutorService executorService = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                    new ThreadFactoryImpl("get new Channel when closed"));
            executorService.execute(() -> {
                Channel channel1 = null;
                try {
                    channel1 = NettyClient.getChannel(hostAndPort.getHost(), hostAndPort.getPort());
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
                hostAndPortChannelsMap.put(hostAndPort, channel1);
                updateChannelWhenClosed(hostAndPort, channel1);
                executorService.shutdown();
            });
        });
    }

    /**
     * Description ：每四秒发送一个心跳包
     **/
    private void hearBeat() {
        while (true) {
            byte[] emptyPackage = new byte[0];
            for (Map.Entry<?, Channel> entry : hostAndPortChannelsMap.entrySet()) {
                entry.getValue().writeAndFlush(emptyPackage);
            }
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}

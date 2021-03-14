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

import com.lzp.zprpc.client.netty.NettyClient;
import com.lzp.zprpc.common.constant.Cons;
import com.lzp.zprpc.common.util.ThreadFactoryImpl;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Description:固定数量连接池实现类
 *
 * @author: Lu ZePing
 * @date: 2020/10/9 17:53
 */
public class ServiceChannelPoolImp implements FixedShareableChannelPool {
    private final Logger LOGGER = LoggerFactory.getLogger(ServiceChannelPoolImp.class);
    private ThreadPoolExecutor heartBeatThreadPool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryImpl("heartBeat"));


    private Map<String, List<Channel>> hostAndPortChannelsMap = new HashMap<>();
    private final int SIZE;

    {
        heartBeatThreadPool.execute(this::hearBeat);
    }

    public ServiceChannelPoolImp(int size) {
        this.SIZE = size;
    }

    @Override
    public Channel getChannel(String hostAndPort)  {
        List<Channel> channels = hostAndPortChannelsMap.get(hostAndPort);
        if (channels == null) {
            synchronized (this) {
                if ((channels = hostAndPortChannelsMap.get(hostAndPort)) == null) {
                    /*用CopyOnWriteArrayList而不用ArrayList来保存channel的原因：
                     * 1、synchronized关键词虽然保证原子性和可见性，但是不能防止指令重排序。需要加volatile来防止半初始化问题
                     *    CopyOnWriteArrayList底层的数组刚好就用volatile修饰了。
                     * 2、当连接池满了，基本都是读操作，基本不会有写操作。CopyOnWriteArrayList的读操作相比ArrayList少了校验
                     *    步骤，能弥补一些因volatile修饰而损失的性能，总体读性能和ArrayList差不多。
                     * */
                    channels = new CopyOnWriteArrayList<>();
                    Channel channel = NettyClient.getChannel(hostAndPort.split(Cons.COLON)[0], Integer.parseInt(hostAndPort.split(Cons.COLON)[1]));
                    updateChannelWhenClosed(channel, channels, hostAndPort);
                    channels.add(channel);
                    hostAndPortChannelsMap.put(hostAndPort, channels);
                    return channel;
                } else if (channels.size() < SIZE) {
                    Channel channel = NettyClient.getChannel(hostAndPort.split(Cons.COLON)[0], Integer.parseInt(hostAndPort.split(Cons.COLON)[1]));
                    updateChannelWhenClosed(channel, channels, hostAndPort);
                    channels.add(channel);
                    return channel;
                } else {
                    return channels.get(ThreadLocalRandom.current().nextInt(SIZE));
                }
            }
        } else if (channels.size() < SIZE) {
            synchronized (this) {
                if ((channels = hostAndPortChannelsMap.get(hostAndPort)).size() < SIZE) {
                    Channel channel = NettyClient.getChannel(hostAndPort.split(Cons.COLON)[0], Integer.parseInt(hostAndPort.split(Cons.COLON)[1]));
                    updateChannelWhenClosed(channel, channels, hostAndPort);
                    channels.add(channel);
                    return channel;
                } else {
                    return channels.get(ThreadLocalRandom.current().nextInt(SIZE));
                }
            }
        } else {
            return channels.get(ThreadLocalRandom.current().nextInt(SIZE));
        }
    }

    /**
     * Description:给channel添加事件监听，当连接不可用了，用新的channel替换
     *
     * @author: Lu ZePing
     * @date: 2020/9/27 18:32
     */
    private void updateChannelWhenClosed(Channel channel, List<Channel> channels, String hostAndPort) {
        /*因为getChannel()会调用ChannelFuture.sync()方法，会阻塞当前线程，不能在io线程中执行下面的代码块。而事件回调却在io线程中执行的
          所以下面这段代码需要在另一个线程中执行。每次都new新线程池是因为连接不可用是小概率事件，线程一直存在会比较耗资源。*/
        ExecutorService executorService = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryImpl("get new Channel when closed"));
        channel.closeFuture().addListener(future -> executorService.execute(() -> {
            synchronized (this) {
                channels.remove(channel);
                Channel channel1 = NettyClient.getChannel(hostAndPort.split(Cons.COLON)[0], Integer.parseInt(hostAndPort.split(Cons.COLON)[1]));
                channels.add(channel1);
                updateChannelWhenClosed(channel1, channels, hostAndPort);
            }
            executorService.shutdown();
        }));
    }

    /**
     * Description ：每12秒发送一个心跳包
     **/
    private void hearBeat() {
        while (true) {
            byte[] emptyPackage = new byte[0];
            for (Map.Entry<String, List<Channel>> entry : hostAndPortChannelsMap.entrySet()) {
                for (Channel channel : entry.getValue()) {
                    channel.writeAndFlush(emptyPackage);
                }
            }
            try {
                Thread.sleep(12000);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}

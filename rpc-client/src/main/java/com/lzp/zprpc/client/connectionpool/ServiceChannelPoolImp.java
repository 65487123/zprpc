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
import com.lzp.zprpc.common.util.StringUtil;
import com.lzp.zprpc.common.util.ThreadFactoryImpl;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
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
    public Channel getChannel(String hostAndPort) throws ConnectException {
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
                    hostAndPortChannelsMap.put(hostAndPort, channels);
                    return initAddAndReturnNewChannel(hostAndPort, channels);
                } else if (channels.size() < SIZE) {
                    return initAddAndReturnNewChannel(hostAndPort, channels);
                } else {
                    return channels.get(ThreadLocalRandom.current().nextInt(SIZE));
                }
            }
        } else if (channels.size() < SIZE) {
            synchronized (this) {
                if ((channels = hostAndPortChannelsMap.get(hostAndPort)).size() < SIZE) {
                    return initAddAndReturnNewChannel(hostAndPort, channels);
                } else {
                    return channels.get(ThreadLocalRandom.current().nextInt(SIZE));
                }
            }
        } else {
            return channels.get(ThreadLocalRandom.current().nextInt(SIZE));
        }
    }

    private Channel initAddAndReturnNewChannel(String hostAndPort, List<Channel> channels) throws ConnectException {
        String[] ipAndPort;
        Channel channel = ConnectionFactory.newChannel((ipAndPort = StringUtil.stringSplit(hostAndPort,
                Cons.COLON))[0], Integer.parseInt(ipAndPort[1]));
        removeChannelWhenClosed(channel, channels);
        channels.add(channel);
        return channel;
    }

    /**
     * Description:给channel添加事件监听，当连接不可用了，用新的channel替换
     *
     * @author: Lu ZePing
     * @date: 2020/9/27 18:32
     */
    private void removeChannelWhenClosed(Channel channel, List<Channel> channels) {
        channel.closeFuture().addListener(future -> {
            synchronized (this) {
                channels.remove(channel);
            }
        });
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

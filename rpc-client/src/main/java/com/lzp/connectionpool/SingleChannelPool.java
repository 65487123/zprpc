package com.lzp.connectionpool;

import com.lzp.ServiceFactory;
import com.lzp.netty.NettyClient;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Description:单线程的线程池
 *
 * @author: Zeping Lu
 * @date: 2020/10/13 16:34
 */
public class SingleChannelPool implements FixedShareableChannelPool {
    private Map<ServiceFactory.HostAndPort, Channel> hostAndPortChannelsMap = new HashMap<>();

    @Override
    public Channel getChannel(ServiceFactory.HostAndPort hostAndPort) throws InterruptedException {
        Channel channel = hostAndPortChannelsMap.get(hostAndPort);
        if (channel == null) {
            synchronized (this) {
                if ((channel = hostAndPortChannelsMap.get(hostAndPort)) == null) {
                    channel = NettyClient.getChannel(hostAndPort.getHost(), hostAndPort.getPort());
                    hostAndPortChannelsMap.put(hostAndPort, channel);
                }
                return channel;
            }
        } else {
            return channel;
        }
    }
}

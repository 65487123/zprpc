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
 * Description:固定数量连接池实现类
 *
 * @author: Lu ZePing
 * @date: 2020/10/9 17:53
 */
public class ServiceChannelPoolImp implements FixedShareableChannelPool {
    private Map<ServiceFactory.HostAndPort, List<Channel>> hostAndPortChannelsMap = new HashMap<>();
    private final int SIZE;

    public ServiceChannelPoolImp(int size) {
        this.SIZE = size;
    }

    @Override
    public Channel getChannel(ServiceFactory.HostAndPort hostAndPort) throws InterruptedException {
        List<Channel> channels = hostAndPortChannelsMap.get(hostAndPort);
        if (channels == null) {
            synchronized (this) {
                if ((channels = hostAndPortChannelsMap.get(hostAndPort)) == null) {
                    /*用CopyOnWriteArrayList而不用ArrayList来保存channel的原因：
                    * 1、synchronized关键词虽然保证原子性和可见性，但是不能防止指令重排序。需要加volatile来防止半初始化问题
                    *    CopyOnWriteArrayList底层的数组刚好就用volatile修饰了。
                    * 2、当连接池满了，基本都是读操作，基本不会有写操作。CopyOnWriteArrayList的读操作相比ArrayList少了校验
                    *    步骤，所以性能比ArrayList高。
                    * */
                    channels = new CopyOnWriteArrayList<>();
                    Channel channel = NettyClient.getChannel(hostAndPort.getHost(), hostAndPort.getPort());
                    channels.add(channel);
                    hostAndPortChannelsMap.put(hostAndPort,channels);
                    return channel;
                } else if (channels.size() < SIZE) {
                    Channel channel = NettyClient.getChannel(hostAndPort.getHost(), hostAndPort.getPort());
                    channels.add(channel);
                    return channel;
                } else {
                    return channels.get(ThreadLocalRandom.current().nextInt(SIZE));
                }
            }
        } else if (channels.size() < SIZE) {
            synchronized (this) {
                if ((channels = hostAndPortChannelsMap.get(hostAndPort)).size() < SIZE) {
                    Channel channel = NettyClient.getChannel(hostAndPort.getHost(), hostAndPort.getPort());
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
}

package com.lzp;


/**
 * Description:固定连接数的连接池，每个ip+port 缓存固定数量的连接。
 * 根据ip+port 和 serviceId 获得一个channel。获取channel后，池子中
 * 这个channel还是在的，并不会真的被取出。
 *
 *
 * @author: Lu ZePing
 * @date: 2020/10/9 17:53
 */
public interface FixedShareableChannelPool {
}

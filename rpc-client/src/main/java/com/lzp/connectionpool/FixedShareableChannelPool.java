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


import com.lzp.ServiceFactory;
import io.netty.channel.Channel;

/**
 * Description:固定连接数的连接池，每个ip+port 缓存固定数量的连接。
 * 根据ip+port 获得一个channel。获取channel后，池子中
 * 这个channel还是在的，并不会真的被取出。
 *
 * @author: Lu ZePing
 * @date: 2020/10/9 17:53
 */
public interface FixedShareableChannelPool {

    /**
     * @return
     * @Author Zeping Lu
     * @Description 根据ip和端口从连接池中获取一个连接，如果已有连接个数没有达到配置的个数，会新建立连接，放入池中并返回。
     * 如果数量达到配置数了，选择一个连接返回。
     * @Date 10:24 2020/10/13
     * @Param hostAndPort ip和端口
     */
    Channel getChannel(ServiceFactory.HostAndPort hostAndPort) throws InterruptedException;
}

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
 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.LinkedBlockingQueue;
 import java.util.concurrent.ThreadPoolExecutor;
 import java.util.concurrent.TimeUnit;

/**
 * Description:单连接的连接池
 *
 * @author: Zeping Lu
 * @date: 2020/10/13 16:34
 */
public class SingleChannelPool implements FixedShareableChannelPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleChannelPool.class);
    private Map<String, Channel> hostAndPortChannelsMap = new ConcurrentHashMap<>();



    @Override
    public Channel getChannel(String hostAndPort) throws ConnectException {
        Channel channel = hostAndPortChannelsMap.get(hostAndPort);
        if (channel == null) {
            synchronized (this) {
                if ((channel = hostAndPortChannelsMap.get(hostAndPort)) == null) {
                    String[] ipAndPort;
                    channel = ConnectionFactory.newChannel((ipAndPort = StringUtil
                            .stringSplit(hostAndPort, Cons.COLON))[0], Integer.parseInt(ipAndPort[1]));
                    channel.closeFuture().addListener(future -> hostAndPortChannelsMap.remove(hostAndPort));
                    hostAndPortChannelsMap.put(hostAndPort, channel);
                }
                return channel;
            }
        } else {
            return channel;
        }
    }

}

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

package com.lzp.zprpc.common.zpproto;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author zeping lu
 */
public class LzpMessageDecoder extends ReplayingDecoder<Void> {
    private final boolean isServer;

    public LzpMessageDecoder(boolean isServer) {
        this.isServer = isServer;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        int length = byteBuf.readInt();
        if (length == 0) {
            if (isServer) {
                channelHandlerContext.channel().writeAndFlush(new byte[0]);
            }
            return;
        }
        byte[] content = new byte[length];
        //这里byteBuf默认是池化的DirectByteBuf,也就是存在堆外直接内存的bytebuf,
        //由于netty用到了DMA技术,当读取网络数据时,数据会从网卡直接被传输到socket缓冲区(内核空间)中,
        //然后netty会把数据从socket缓冲区读取到堆外DirectByteBuf中(先读取到javaNIO的buffer中，然后包装成DirectByteBuf),
        //然后我下面这行代码，会把堆外的DirectByteBuf中的数据拷贝到堆内的数组中。
        //readBytes(content)底层会调用getBytes(readerIndex, dst, dstIndex, length),然后把读索引加个数组长度;
        byteBuf.readBytes(content);
        /*个人理解,DirectByteBuf相比堆内的ByteBuffer只是少了一次从堆外(用户空间)拷贝到堆内ByteBuf的过程
        * 因为不管netty用的是堆外Buf还是堆内Buf,数据都是必须要先传输到用户空间的堆外内存(必须传输到用户空间是因为
        * 用户进程是没法直接访问内核空间的数据的，只能通过系统调用间接访问，每次访问，程序都会从用户态切换为内核态，效率很低，
        * 所以数据直接传输到用户空间，访问效率高点。必须先传到堆外是因为在数据传输过程中，目标地址不能变,而堆内的对象地址是会
        * 随着GC而变化的)
        * */
        list.add(content);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();
        }
    }

}

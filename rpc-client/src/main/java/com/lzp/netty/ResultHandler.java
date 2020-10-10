package com.lzp.netty;

import com.lzp.util.ThreadFactoryImpl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Description:处理服务端返回的RPC调用结果
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 21:31
 */
public class ResultHandler extends SimpleChannelInboundHandler<byte[]> {
    /**
     * Description:用来存超时时刻和线程，这个类的对象占24字节
     */
    static class ThreadWithTime{
        /**
         * Description:long 基本类型存过期时间， 占8个字节
         */
        private long deadLine;
        /**
         * Description:线程对象的引用， 指针压缩后占4个字节
         * 属性总共12字节，加上指针压缩后的对象头12字节， 刚好24字节，都不需要对其填充。
         */
        private Thread blockedThread;

        ThreadWithTime(long deadLine,Thread blockedThread){
            this.deadLine = deadLine;
            this.blockedThread = blockedThread;
        }
    }

    /**
     * Description:key是rpc请求id，value是待唤醒的线程和超时时间
     */
    private static Map<String,Thread> reqIdThreadMap = new HashMap<>();

    private ExecutorService checkTimeoutThreadPoll = new ThreadPoolExecutor(1,1,0, TimeUnit.SECONDS,new LinkedBlockingQueue<>(),new ThreadFactoryImpl("timeoutCheck"));
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) throws Exception {

    }
}

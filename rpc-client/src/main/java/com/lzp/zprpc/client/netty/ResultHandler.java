package com.lzp.zprpc.client.netty;

import com.lzp.zprpc.common.constant.Cons;
import com.lzp.zprpc.common.dtos.ResponseDTO;
import com.lzp.zprpc.common.util.ResponseSearialUtil;
import com.lzp.zprpc.common.util.ThreadFactoryImpl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * Description:处理服务端返回的RPC调用结果
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 21:31
 */
public class ResultHandler extends SimpleChannelInboundHandler<byte[]> {

    /**
     * Description:用来存超时时刻和线程以及rpc结果
     */
    public static class ThreadResultAndTime {
        /**
         * 过期的具体时刻
         */
        private long deadLine;
        /**
         * 被阻塞的线程
         */
        private Thread thread;
        /**
         * rpc结果
         */
        private volatile Object result;

        public ThreadResultAndTime(long deadLine, Thread thread) {
            this.deadLine = deadLine;
            this.thread = thread;
        }

        public Object getResult() {
            return result;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultHandler.class);

    /**
     * Description:线程池
     */
    private static ExecutorService rpcClientThreadPool;
    /**
     * Description:key是发起rpc请求后被阻塞的线程id，value是待唤醒的线程和超时时间
     */
    public static Map<Long, ThreadResultAndTime> reqIdThreadMap = new ConcurrentHashMap<>();


    static {
        rpcClientThreadPool = new ThreadPoolExecutor(3, 3, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000),
                new ThreadFactoryImpl("rpc client"), (r, executor) -> r.run());



        //一个线程专门用来检测rpc超时
        rpcClientThreadPool.execute(() -> {
            long now;
            while (true) {
                now = System.currentTimeMillis();
                for (Map.Entry<Long, ThreadResultAndTime> entry : reqIdThreadMap.entrySet()) {
                    //漏网之鱼会在下次被揪出来
                    if (entry.getValue().deadLine < now) {
                        ThreadResultAndTime threadResultAndTime = reqIdThreadMap.remove(entry.getKey());
                        threadResultAndTime.result = Cons.EXCEPTION + Cons.TIMEOUT;
                        LockSupport.unpark(threadResultAndTime.thread);
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(),e);
                }
            }
        });
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) {
        rpcClientThreadPool.execute(() -> {
            ResponseDTO responseDTO = ResponseSearialUtil.deserialize(bytes);
            ThreadResultAndTime threadResultAndTime = reqIdThreadMap.remove(responseDTO.getThreadId());
            if (threadResultAndTime != null) {
                threadResultAndTime.result = responseDTO.getResult();
                LockSupport.unpark(threadResultAndTime.thread);
            }
        });
    }
}

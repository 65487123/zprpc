package zprpc.demo.producer;

import com.lzp.netty.Server;

import java.util.concurrent.CountDownLatch;


/**
 * @author zeping lu
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        Server.startRpcServer(5555);
        new CountDownLatch(1).await();
    }
}

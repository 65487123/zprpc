package com.lzp.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description:线程工厂
 *
 * @author: Lu ZePing
 * @date: 2019/6/2 15:19
 */
public class ThreadFactoryImpl implements ThreadFactory {
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    public ThreadFactoryImpl(String namePrefix) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        this.namePrefix = namePrefix + "-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(group, r,
                namePrefix + threadNumber.getAndIncrement(),
                0);
    }

}
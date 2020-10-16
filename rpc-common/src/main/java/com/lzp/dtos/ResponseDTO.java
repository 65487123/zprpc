package com.lzp.dtos;


/**
 * Description:返回结果对象
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 15:24
 */
public class ResponseDTO {
    /**
     * rpc调用结果
     */
    private Object result;
    /**
     * 发起rpc请求的线程的线程id。
     * 不用包装类型原因：
     * 1为了性能，自动装箱需要new一次对象
     * 2这个对象只用作自己定义的底层协议，业务场景不会出现阿里规范里说的情况，。
     */
    private long threadId;

    public Object getResult() {
        return result;
    }

    public long getThreadId() {
        return threadId;
    }

    public ResponseDTO() {
    }

    public ResponseDTO(Object result, long threadId) {
        this.result = result;
        this.threadId = threadId;
    }

    @Override
    public String toString() {
        return "ResponseDTO{" +
                "result=" + result +
                ", threadId=" + threadId +
                '}';
    }
}

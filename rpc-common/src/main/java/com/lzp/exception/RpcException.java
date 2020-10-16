package com.lzp.exception;

/**
 * Description:远程调用出现异常
 *
 * @author: Zeping Lu
 * @date: 2020/10/16 20:40
 */
public class RpcException extends RuntimeException {
    public RpcException(String message) {
        super(message);
    }
}

package com.lzp.dtos;

import java.lang.reflect.Method;

/**
 * Description:RPC请求对象
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 14:16
 */
public class RequestDTO {
    /**
     * 被调用的serviceId
     */
    private String serviceId;
    /**
     * 被调用的方法
     */
    private Method method;
    /**
     * 调用参数
     */
    private Object[] prams;

    public RequestDTO() {
    }

    public RequestDTO(String serviceId, Object... prams) {
        this.serviceId = serviceId;
        this.prams = prams;
    }

    public Method getMethod() {
        return method;
    }

    public String getServiceId() {
        return serviceId;
    }

    public Object[] getPrams() {
        return prams;
    }

    @Override
    public String toString() {
        return "RequestDTO{" +
                "serviceId='" + serviceId + '\'' +
                ", method=" + method +
                ", prams=" + prams +
                '}';
    }
}

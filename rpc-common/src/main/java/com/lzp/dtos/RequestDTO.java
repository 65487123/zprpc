package com.lzp.dtos;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Description:RPC请求对象
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 14:16
 */
public class RequestDTO {
    private String serviceId;
    private Method method;
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

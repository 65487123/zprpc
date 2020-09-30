package com.lzp.protocol.zpproto;

import java.io.Serializable;
import java.util.List;

/**
 * Description:RPC请求对象
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 14:16
 */
public class RequestDTO {
    private String serviceId;
    private List<Object> prams;

    public RequestDTO(){}

    public RequestDTO(String serviceId, List<Object> prams) {
        this.serviceId = serviceId;
        this.prams = prams;
    }

    public String getServiceId() {
        return serviceId;
    }

    public List<Object> getPrams() {
        return prams;
    }

    @Override
    public String toString() {
        return "RequestDTO{" +
                "serviceId='" + serviceId + '\'' +
                ", prams=" + prams +
                '}';
    }
}

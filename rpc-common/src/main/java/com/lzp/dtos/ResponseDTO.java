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
     * rpc调用的requestId
     */
    private String reqId;

    public Object getResult() {
        return result;
    }

    public String getReqId() {
        return reqId;
    }

    public ResponseDTO() {
    }

    public ResponseDTO(Object result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "ResponseDTO{" +
                "result=" + result +
                '}';
    }
}

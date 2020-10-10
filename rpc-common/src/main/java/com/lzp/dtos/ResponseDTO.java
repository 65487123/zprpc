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
     * 被调用的serviceId
     */
    private String id;

    public Object getResult() {
        return result;
    }

    public String getId() {
        return id;
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

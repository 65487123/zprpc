package com.lzp.dtos;

import java.io.Serializable;

/**
 * Description:
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 15:24
 */
public class ResponseDTO {
    private Object result;

    public Object getResult() {
        return result;
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

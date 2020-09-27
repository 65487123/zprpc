package com.lzp.annotation;

/**
 * Description:用来注入远程服务对象
 *
 * @author: Lu ZePing
 * @date: 2020/9/27 12:32
 */
public @interface Reference {
    String id();
    String interfaceValue();
}

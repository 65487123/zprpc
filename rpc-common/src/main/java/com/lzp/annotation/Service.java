package com.lzp.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Description:用来注册服务
 *
 * @author: Lu ZePing
 * @date: 2020/9/27 12:32
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface Service {
    String id();
    String interfaceValue();
}

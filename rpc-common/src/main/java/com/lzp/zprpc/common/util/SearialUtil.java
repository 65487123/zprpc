package com.lzp.zprpc.common.util;

import com.lzp.zprpc.common.dtos.RequestDTO;
import com.lzp.zprpc.common.dtos.ResponseDTO;
import io.fury.Fury;
import io.fury.Language;
import io.fury.ThreadLocalFury;
import io.fury.ThreadSafeFury;


public class SearialUtil {
    private static final ThreadSafeFury threadSafeFury = new ThreadLocalFury(classLoader -> {
        Fury f = Fury.builder().withLanguage(Language.JAVA)
                .withNumberCompressed(true).withJdkClassSerializableCheck(false)
                .requireClassRegistration(false).build();
        f.register(Class[].class);
        f.register(RequestDTO.class);
        f.register(ResponseDTO.class);
        f.register(Object[].class);
        return f;
    });

    public static byte[] serialize(Object user) {
        // Serializes the {@code message} into a byte array using the given schema

        return threadSafeFury.serialize(user);
        //return serialize(user);
    }

    /**
     * 反序列化方法，将字节数组反序列化为RequestDTO对象
     *
     * @param array
     * @return
     */
    public static Object deserialize(byte[] array) {
        return threadSafeFury.deserialize(array);
        //return  deserialize(array);
    }
}

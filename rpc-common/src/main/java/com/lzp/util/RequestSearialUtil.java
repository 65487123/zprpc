package com.lzp.util;

import com.lzp.protocol.zpproto.RequestDTO;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.runtime.RuntimeSchema;

/**
 * Description:
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 14:03
 */
public class RequestSearialUtil {

        private static RuntimeSchema<RequestDTO> schema = RuntimeSchema.createFrom(RequestDTO.class);

        /**
         * 序列化方法，将RequestDTO对象序列化为字节数组
         *
         * @param user
         * @return
         */
        public static byte[] serialize(RequestDTO user) {
            // Serializes the {@code message} into a byte array using the given schema
            return ProtostuffIOUtil.toByteArray(user, schema, LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
        }

        /**
         * 反序列化方法，将字节数组反序列化为RequestDTO对象
         *
         * @param array
         * @return
         */
        public static RequestDTO deserialize(byte[] array) {
            RequestDTO user = schema.newMessage();
            // Merges the {@code message} with the byte array using the given {@code schema}
            ProtostuffIOUtil.mergeFrom(array, user, schema);
            return user;
        }
}

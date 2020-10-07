package com.lzp.util;

import com.lzp.dtos.ResponseDTO;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.runtime.RuntimeSchema;

/**
 * Description:
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 14:03
 */
public class ResponseSearialUtil {

        private static RuntimeSchema<ResponseDTO> schema = RuntimeSchema.createFrom(ResponseDTO.class);

        /**
         * 序列化方法，将Object对象序列化为字节数组
         *
         * @param response
         * @return
         */
        public static byte[] serialize(ResponseDTO response) {
            // Serializes the {@code message} into a byte array using the given schema
            return ProtostuffIOUtil.toByteArray(response, schema, LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
        }

        /**
         * 反序列化方法，将字节数组反序列化为Object对象
         *
         * @param array
         * @return
         */
        public static ResponseDTO deserialize(byte[] array) {
            ResponseDTO response = schema.newMessage();
            // Merges the {@code message} with the byte array using the given {@code schema}
            ProtostuffIOUtil.mergeFrom(array, response, schema);
            return response;
        }
}

 /* Copyright zeping lu
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */

package com.lzp.util;

import com.lzp.dtos.RequestDTO;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.runtime.RuntimeSchema;

/**
 * Description:序列化、反序列化请求对象的工具
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

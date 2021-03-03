
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

package com.lzp.zprpc.registry.api;

import java.util.List;

/**
 * Description:单机redis以及集群redis的统一客户端
 *
 * @author: Zeping Lu
 * @date: 2021/3/2 16:59
 */
public interface RedisClient extends AutoCloseable {

    /**
     * @Description 对应redis的lpush
     */
    Long sAdd(String var1, String... var2);

    /**
     * @Description 根据key获取Set类型的value并返回List
     */
    List<String> getAndTransformToList(String key);

    /**
     * @Description 删除list中的第一个匹配元素
     */
    void sremove(String key,String Value);
}

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

package com.lzp.exception;

/**
 * Description:没有空闲端口异常，当不指定端口时发布服务可能会抛出
 *
 * @author: Zeping Lu
 * @date: 2021/1/26 9:43
 */
public class NoFreePortException extends RuntimeException{
    public NoFreePortException(String message) {
        super(message);
    }

}

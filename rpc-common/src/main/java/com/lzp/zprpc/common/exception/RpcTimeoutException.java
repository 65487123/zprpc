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

package com.lzp.zprpc.common.exception;

/**
 * Description:远程调用超时异常,当指定了超时时间发起远程调用,超过这个时间还没返回结果,就会抛出这个异常
 *
 * @author: Zeping Lu
 * @date: 2020/10/16 20:40
 */
public class RpcTimeoutException extends RuntimeException {
    public RpcTimeoutException(String message) {
        super(message);
    }
}

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
 * Description:在发起远程调用时候出现异常(还没有真正进行远程调用),会返回这个异常
 *
 *  目前就一种情况会返回这个异常
 *  没有可用服务时：调用rpc服务时，发现没有任何可用服务
 *
 * @author: Zeping Lu
 * @date: 2021/2/27 17:55
 */
public class CallException extends RuntimeException {
    public CallException(String message) {
        super(message);
    }
}

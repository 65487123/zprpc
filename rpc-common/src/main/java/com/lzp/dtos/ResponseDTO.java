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

package com.lzp.dtos;


/**
 * Description:返回结果对象
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 15:24
 */
public class ResponseDTO {
    /**
     * rpc调用结果
     */
    private Object result;
    /**
     * 发起rpc请求的线程的线程id。
     * 不用包装类型原因：
     * 1为了性能，自动装箱需要new一次对象
     * 2这个对象只用作自己定义的底层协议，业务场景不会出现阿里规范里说的情况，。
     */
    private long threadId;

    public Object getResult() {
        return result;
    }

    public long getThreadId() {
        return threadId;
    }

    public ResponseDTO() {
    }

    public ResponseDTO(Object result, long threadId) {
        this.result = result;
        this.threadId = threadId;
    }

    @Override
    public String toString() {
        return "ResponseDTO{" +
                "result=" + result +
                ", threadId=" + threadId +
                '}';
    }
}

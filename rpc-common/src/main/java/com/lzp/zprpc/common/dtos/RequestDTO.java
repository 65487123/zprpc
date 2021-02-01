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

package com.lzp.zprpc.common.dtos;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Description:RPC请求对象
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 14:16
 */
public class RequestDTO {
    /**
     * 发起rpc请求的线程的线程id。
     * 不用包装类型原因：
     * 1为了性能，自动装箱需要new一次对象
     * 2这个对象只用作自己定义的底层协议，业务场景不会出现阿里规范里说的情况，。
     */
    private long threadId;
    /**
     * 被调用的方法
     */
    private Method method;
    /**
     * 调用参数
     */
    private Object[] prams;
    /**
     * 调用的服务id
     */
    private String serviceId;

    public RequestDTO() {
    }

    public RequestDTO(long threadId, String serviceId, Method method, Object... prams) {
        this.threadId = threadId;
        this.prams = prams;
        this.serviceId = serviceId;
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }


    public Object[] getPrams() {
        return prams;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setPrams(Object[] prams) {
        this.prams = prams;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public String toString() {
        return "RequestDTO{" +
                "threadId=" + threadId +
                ", method=" + method +
                ", prams=" + Arrays.toString(prams) +
                ", serviceId='" + serviceId + '\'' +
                '}';
    }
}

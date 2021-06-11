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


import java.util.Map;

/**
 * Description:注册中心客户端统一接口
 *
 * @author: Lu ZePing
 * @date: 2020/9/27 18:32
 */
public interface RegistryClient extends AutoCloseable{


    /**
     * 扫描指定包下所有类，获得所有被com.lzp.annotation.@Service修饰的类，返回实例。如果项目用到了Spring，就到
     * Spring容器中找，找不到才自己初始化一个,并注册到注册中心中(jar包需要打到同一classpath下)
     *
     *
     *
     * @param basePack 要扫描的包
     * @param ip       要注册进注册中心的实例（instance)ip
     * @param port     要注册进注册中心的实例（instance)port
     * @return key是服务的唯一id，value是实例
     */
    Map<String, Object> searchAndRegiInstance(String basePack, String ip, int port) throws Exception;


}

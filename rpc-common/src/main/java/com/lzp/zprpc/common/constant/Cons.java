
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

package com.lzp.zprpc.common.constant;

/**
 * Description:常量类
 *  牺牲微小的内存占用，提高可读、可维护性。
 *  别的类用到这个类里的常量，在编译的时候就会把这个类里的常量编译到那个类的class常量池中。
 *
 * @author: Zeping Lu
 * @date: 2021/1/18 14:49
 */
public class Cons {
    public static final String TIMEOUT = "timeout";
    public static final String EXCEPTION = "exceptionÈ";
    public static final int TEN = 10;
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 49152;
    public static final String DOCKER_NAME = "docker";
}

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


package com.lzp.zprpc.common.util;


/**
 * Description:字符串工具类
 *
 * @author: Lu ZePing
 * @date: 2019/6/2 15:19
 */
public class StringUtil {

    /**
     * 分割字符串,只支持分割后只有两个元素
     */

    public static String[] stringSplit(String string, char c) {
        String[] strings = new String[2];
        for (int i = 0; i < 2; i++) {
            int k = string.indexOf(c);
            if (k < 0) {
                strings[i] = string;
                break;
            }
            String s = string.substring(0, k);
            strings[i] = s;
            string = string.substring(k + 1);
        }
        return strings;
    }

}

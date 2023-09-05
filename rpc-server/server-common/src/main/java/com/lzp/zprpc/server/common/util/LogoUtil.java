
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

package com.lzp.zprpc.server.common.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Description:启动时打印logo
 *
 * @author: Zeping Lu
 * @date: 2021/1/27 12:10
 */
public class LogoUtil {
    public static void printLogo() {
        byte[] bytes;
        try {
            InputStream inputStream = LogoUtil.class.getClassLoader().getResourceAsStream("banner.txt");
            inputStream.read(bytes = new byte[inputStream.available()]);
            System.out.println(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }
}

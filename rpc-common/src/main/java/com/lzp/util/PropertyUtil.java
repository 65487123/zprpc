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

package com.lzp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Description:获取配置文件里的属性
 *
 * @author: Lu ZePing
 * @date: 2020/9/28 10:44
 */
public class PropertyUtil {
    private static final Logger logger = LoggerFactory.getLogger(PropertyUtil.class);
    private static Properties properties = new Properties();

    static {
        InputStream in = PropertyUtil.class.getClassLoader().getResourceAsStream("zprpc.properties");
        try {
            properties.load(in);
        } catch (IOException e) {
            logger.error("zprpc.properties was not found", e);
        }
    }


    public static String getBasePack() {
        return properties.getProperty("basePack");
    }

    public static String getConnetionPoolSize() {
        return properties.getProperty("connetionPoolSize");
    }


    public static String getNacosIpList() {
        return properties.getProperty("nacosIpList");
    }

}

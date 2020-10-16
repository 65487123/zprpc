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
            logger.error(e.getMessage(), e);
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

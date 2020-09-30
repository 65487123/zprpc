package com.lzp.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description:扫描指定包下所有类，获得所有被指定注解修饰的类，并返回实例（如果项目用到了Spring，就到
 * Spring 容器中找，找不到才自己初始化一个)
 *
 * @author: Lu ZePing
 * @date: 2020/9/30 11:14
 */
public class AnnotationUtil {


    public static Map searchClass(Class annotationClass) throws ClassNotFoundException {
        Map<String, Object> map = new HashMap();
        //包名
        String basePack = "com.baibin";
        //先把包名转换为路径,首先得到项目的classpath
        String classpath = AnnotationUtil.class.getResource("/").getPath();
        //然后把我们的包名basPach转换为路径名
        basePack = basePack.replace(".", File.separator);
        //然后把classpath和basePack合并
        String searchPath = classpath + basePack;
        List<String> classPaths = new ArrayList<String>();
        doPath(new File(searchPath), classPaths);
        //这个时候我们已经得到了指定包下所有的类的绝对路径了。我们现在利用这些绝对路径和java的反射机制得到他们的类对象
        for (String s : classPaths) {
            //把 D:\work\code\20170401\search-class\target\classes\com\baibin\search\a\A.class 这样的绝对路径转换为全类名com.baibin.search.a.A
            s = s.replace(classpath.replace("/", "\\").replaceFirst("\\\\", ""), "").replace("\\", ".").replace(".class", "");
            Class cls = Class.forName(s);

        }
        return map;
    }

    /**
     * 该方法会得到所有的类，将类的绝对路径写入到容器中
     * @param file
     */
    private static void doPath(File file, List<String> classPaths) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f1 : files) {
                doPath(f1,classPaths);
            }
        } else {
            //标准文件我们就判断是否是class文件
            if (file.getName().endsWith(".class")) {
                classPaths.add(file.getPath());
            }
        }
    }
}

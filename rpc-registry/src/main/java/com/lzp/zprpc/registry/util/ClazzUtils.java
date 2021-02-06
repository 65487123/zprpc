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

package com.lzp.zprpc.registry.util;

import org.eclipse.core.runtime.FileLocator;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Description:用来扫描指定包下的类
 *
 * @author: Zeping Lu
 * @date: 2020/10/18 15:23
 */
public class ClazzUtils {
    private static final String CLASS_SUFFIX = ".class";
    private static final String PACKAGE_SEPARATOR = ".";
    private static final String FILE = "file";
    private static final String JAR = "jar";
    private static final String DOLLAR = "$";
    private static final String BUNDLE_RESOURCE = "bundleresource";

    /**
     * 查找包下的所有类的名字
     *
     * @param packageName
     * @return List集合，内容为类的全名
     */
    public static List<String> getClazzName(String packageName) {
        return getClazzName0(packageName, ClazzUtils.class.getClassLoader());
    }

    /**
     * 查找包下的所有类的名字
     *
     * @param packageName
     * @return List集合，内容为类的全名
     */
    public static List<String> getClazzName(String packageName,ClassLoader loader) {
        return getClazzName0(packageName,loader);
    }


    private static List<String> getClazzName0(String packageName,ClassLoader loader){
        List<String> result = new ArrayList<>();
        String suffixPath = packageName.replaceAll("\\.", "/");
        try {
            Enumeration<URL> urls = loader.getResources(suffixPath);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url != null) {
                    String protocol = url.getProtocol();
                    if (ClazzUtils.FILE.equals(protocol)) {
                        String path = url.getPath();
                        result.addAll(getAllClassNameByFile(new File(path),packageName));
                    } else if (ClazzUtils.JAR.equals(protocol)) {
                        JarFile jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
                        if (jarFile != null) {
                            result.addAll(getAllClassNameByJar(jarFile, packageName));
                        }
                    } else if (ClazzUtils.BUNDLE_RESOURCE.equals(protocol)) {
                        result.addAll(getAllClassNameByFile(new File(FileLocator.toFileURL(url).getPath()),packageName));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    /**
     * 递归获取所有class文件的名字
     *
     * @param file
     * @return List
     */
    private static List<String> getAllClassNameByFile(File file,String packageName) {
        List<String> result = new ArrayList<>();
        if (file.exists()) {
            if (file.isFile()) {
                findClazzName(file, result, packageName);
            } else {
                File[] listFiles = file.listFiles();
                if (listFiles != null && listFiles.length > 0) {
                    for (File f : listFiles) {
                        result.addAll(getAllClassNameByFile(f,packageName));
                    }
                }
            }
        }
        return result;
    }

    private static void findClazzName(File file, List<String> result, String packageName) {
        String path = file.getPath();
        // 注意：这里替换文件分割符要用replace。因为replaceAll里面的参数是正则表达式,而windows环境中File.separator="\\"的,因此会有问题
        if (path.endsWith(CLASS_SUFFIX)) {
            path = path.replace(CLASS_SUFFIX, "");
            // 从"/classes/"后面开始截取
            String clazzName = (path = path.replace(File.separator, PACKAGE_SEPARATOR)).substring(path.indexOf(packageName));
            if (!clazzName.contains(ClazzUtils.DOLLAR)) {
                result.add(clazzName);
            }
        }
    }

    /**
     * 递归获取jar所有class文件的名字
     *
     * @param jarFile
     * @param packageName 包名
     * @return List
     * 123
     */
    private static List<String> getAllClassNameByJar(JarFile jarFile, String packageName) {
        List<String> result = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String name = jarEntry.getName();
            // 判断是不是class文件
            if (name.endsWith(CLASS_SUFFIX)) {
                name = name.replace(CLASS_SUFFIX, "").replace("/", ".");
                // 如果要子包的文件,那么就只要开头相同且不是内部类就ok
                if (name.startsWith(packageName) && !name.contains(ClazzUtils.DOLLAR)) {
                    result.add(name);
                }
            }
        }
        return result;
    }

}

package com.lzp.util;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.lzp.annotation.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description:扫描指定包下所有类，获得所有被指定注解修饰的类，返回实例（如果项目用到了Spring，就到
 * Spring容器中找，找不到才自己初始化一个),并注册到注册中心中
 * 只会扫描依赖了这个项目的工程的classpath下的包，其classpath下面的jar包里的包是不会扫描的
 *
 * @author: Lu ZePing
 * @date: 2020/9/30 11:14
 */
public class RegisterUtil {

    /**
     * 扫描指定包下所有类，获得所有被com.lzp.com.lzp.annotation.@Service修饰的类，返回实例（如果项目用到了Spring，就到
     *   Spring容器中找，找不到才自己初始化一个),并注册到注册中心中
     *
     *   ______________________________________________
     *   |               namespace                     |
     *   |   ————————————————————————————————————————  |
     *   |  | ____________ group____________________ | |
     *   |  || |------------service--------------| | | |
     *   |  || | |cluser |          | cluster|   | | | |
     *   |  || | |_______|          |________|   | | | |
     *   |  || |_________________________________| | | |
     *   |  ||_____________________________________| | |
     *   |  |_______________________________________ | |
     *   ———————————————————————————————————————————————
     *   group和serviceid决定一个服务，一个service包含多个cluster，每个cluster
     *   里包含多个instance
     * @param basePack  要扫描的包
     * @param namingService 注册中心
     * @param ip  要注册进注册中心的实例（instance)ip
     * @param port  要注册进注册中心的实例（instance)port
     */


    public static Map<String, Object> searchAndRegiInstance(String basePack, NamingService namingService,String ip,int port) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NacosException {
        Map<String, Object> idServiceMap = new HashMap();
        String classpath = RegisterUtil.class.getResource("/").getPath();
        basePack = basePack.replace(".", File.separator);
        String searchPath = classpath + basePack;
        List<String> classPaths = new ArrayList<String>();
        doPath(new File(searchPath), classPaths);
        for (String s : classPaths) {
            s = s.replace(classpath.replace("/", "\\").replaceFirst("\\\\", ""), "").replace("\\", ".").replace(".class", "");
            Class cls = Class.forName(s);
            if (cls.isAnnotationPresent(Service.class)) {
                Service service = (Service) cls.getAnnotation(Service.class);
                Map<String, Object> nameInstanceMap = SpringUtil.getBeansOfType(cls);
                if (nameInstanceMap.size() != 0) {
                    idServiceMap.put(service.id(), nameInstanceMap.entrySet().iterator().next().getValue());
                } else {
                    idServiceMap.put(service.id(), cls.newInstance());
                }
                namingService.registerInstance(service.id(), ip, port);
            }
        }
        return idServiceMap;
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
            if (file.getName().endsWith(".class")) {
                classPaths.add(file.getPath());
            }
        }
    }
}

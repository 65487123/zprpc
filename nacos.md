# nacos做注册中心使用方法

                
### 一、环境搭建(如果环境已搭好，可跳过)
    1、拉取代码到本地,在项目根目录执行maven安装命令:mvn clean install
    2、上传到私服仓库(可跳过,如果做了,后续就不用执行第1步了，直接依赖maven坐标就行)
    3、搭建nacos环境：到github找到nacos源码，拉取到本地，编译并部署。(如果有nacos环境，跳过)
    nacos具体部署操作不熟悉的可以参照nacos官网 
   [nacos官网](https://nacos.io/zh-cn/docs/quick-start.html)
### 二、创建工程、导入依赖、编写配置与代码
##### 定义公共接口
    1、新建一个工程，定义公共接口，供服务提供方和服务消费方依赖
##### 服务提供方
    1、创建服务提供方工程，依赖提供接口的工程，并导入maven依赖
    <dependency>
         <groupId>com.lzp.zprpc</groupId>
         <!--如果既是服务服务方又是服务消费方,artifactId为rpc-artifacts就行-->
         <artifactId>rpc-server</artifactId>
         <version>version</version>
    </dependency>
    2、创建接口实现类，实现接口的具体方法，并在接口实现类上加上com.lzp.zprpc.common.annotation.Service注解;
    注解有两个参数，分别是服务的id和接口的全限定名，服务id需要有唯一性
    示例：@Service(id = "serviceImpl", interfaceValue = "xxx.xxx.xxx.Service")
    3、在resources包下加入配置文件：zprpc.properties，加入配置项。其中有两项是必配的
    (1)需要要扫描的包的路径：basePack。示例：basePack=zprpc.demo.producer
    (2)nacos的ip：nacosIpList。示例：nacosIpList=192.168.0.101:8848
    除了通过配置文件配置发布到的注册中心的地址,还可以通过系统环境变量设置,系统环境变量优先级大于配置文件
    RPC_REGISTRY = xxx.xxx.xxx.xxx:8848
    示例:RPC_REGISTRY = 10.240.70.180:8848,10.240.70.173:8848,10.240.70.166:8848

    4、通过代码启动服务提供方：
    com.lzp.zprpc.server.netty.Server.startRpcServer(ip,port);
    或 Server.startRpcServer(port);
    或 Server.startRpcServer();
    不写ip，默认就是本机ip。ip和port都不写，默认就是本机ip加最小可用端口
    
    服务提供方启动后，会扫描被@Service注解修饰的服务，初始化后保存在本地(都是单例的)，并把服务发布到nacos中。
 
    如果项目用到了spring,并且服务也被注册到了spring容器中,推荐在spring启动类上加入@Import(com.lzp.zprpc.common.util.SpringUtil.class)。
    先启动spring容器然后再启动rpc服务。这样在发布服务时，会先到spring容器中去找，如果spring容器中有服务实例，就会用spring中的。如果没有就会自己初始化一个。
   
    如果集群部署的话，建议同一个服务发布的个数为2的整次方，这样客户端在负载均衡时性能能更高
    
    5、如果想在不关停JVM的情况下关闭RPC服务,可以调用Server.closeRpcServer(long timeToWait, TimeUnit unit)。
    调用后,启动服务时注册进注册中心的服务都会被注销,然后关闭线程池,最后释放server监听的端口,参数即线程池关闭时等待的时间。  
    如果想再次启动服务,再次执行第4步就行。
##### 服务消费方   
    1、创建服务消费方工程，依赖提供接口的工程，并导入依赖
    <dependency>
         <groupId>com.lzp.zprpc</groupId>
         <!--如果既是服务服务方又是服务消费方,artifactId为rpc-artifacts就行-->
         <artifactId>rpc-client</artifactId>
         <version>1.0</version>
    </dependency>
    2、编写配置文件，有一项是必写的：
    nacos的ip：nacosIpList。示例：nacosIpList=192.168.0.101:8848
    还可以配置和每台实例的连接池连接数。
    示例：connetionPoolSize：2
    不配置，默认连接池里的数量就是一。 也就是这个消费方和某个服务实例里的所有服务通信都是走这一个连接，但是不会有任何阻塞。
    推荐不配置连接池连接数，使用默认单个连接的连接池。因为客户端开了一个Reactor，也就是只有一个线程服务所有连接，多个连接没多大意义
    3.得到代理对象，通过代理对象可以发起远程调用，就和调用本地方法一样
    com.lzp.zprpc.client.nacos.ServiceFactory.getServiceBean(String serviceId,Class interfaceCls);
    serviceId就是服务的唯一id，interfaceCls是接口的Class对象。返回一个实例，强转成接口类型就行。
    也可以通过
    ServiceFactory.getServiceBean(String serviceId,Class interfaceCls,int timeOut);
    来获取代理对象，通过这个对象远程调用会有超时限制，超过指定秒数没返回结果就会抛出超时异常。
   
 ### 三、Demo 
    源码中提供了demo，在rpc-demo工程下，包含了服务提供方工程和服务消费方工程，代码拉下来编译后直接就能跑，配置一下nacosIpList，
    先启动服务提供方再启动服务消费方就能看到结果。



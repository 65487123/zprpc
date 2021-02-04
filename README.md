# zprpc
中文|[English](https://github.com/65487123/zprpc/blob/master/README-EN.md)
# 功能介绍
    一个高性能rpc框架，暂时只支持nacos做注册中心。
# 项目整体架构
![architecture](https://github.com/65487123/zprpc/raw/master/architecture.png)
# 特点：
    1、配置简单，上手容易。
    2、适用场景丰富
    (1)启动过程不依赖任何组件，比如tomcat、spring(有没有这些组件，用法都一样)。
    (2)支持用到这个框架的jar和这个框架本身打成的jar不在一个classpath(比如OSGI环境,
    但是建议最好还是打在同一个classpath下,框架本身也轻,打在同一包对性能也没啥影响)。
    3、高性能：网络通信基于netty，序列化用的protostuff。实现了连接池机制，并且同一
    时间多个rpc调用可以共用一个连接(无阻塞)，单例代理对象，代码细节注重性能。                  
# 	使用方法：
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
         <version>1.0</version>
    </dependency>
    2、创建接口实现类，实现接口的具体方法，并在接口实现类上加上com.lzp.zprpc.common.annotation.Service注解;
    注解有两个参数，分别是服务的id和接口的全限定名，服务id需要有唯一性
    示例：@Service(id = "serviceImpl", interfaceValue = "xxx.xxx.xxx.Service")
    3、在resources包下加入配置文件：zprpc.properties，加入配置项。其中有两项是必配的
    (1)需要要扫描的包的路径：basePack。示例：basePack=zprpc.demo.producer
    (2)nacos的ip：nacosIpList。示例：nacosIpList=192.168.0.101:8848
    4、通过代码启动服务提供方：
    com.lzp.zprpc.server.netty.Server.startRpcServer(ip,port);
    或 Server.startRpcServer(port);
    或 Server.startRpcServer();
    不写ip，默认就是本机ip。ip和port都不写，默认就是本机ip加随机可用端口
    一般情况下,自己写的类或配置文件都在classpath下面,通过上述启动方式就能正常发布服务,
    但是如果自己项目里的类或配置文件需要特殊的类加载器才能加载(自定义了类加载器，比如项目基于OSGI框架的,加载不同bundle
    需要不同类加载器)则需要在启动服务方法上加个类加载器参数:
    Server.startRpcServer(ip,port,classLoader);或Server.startRpcServer(port,classLoader);或Server.startRpcServer(classLoader);
    服务提供方启动后，会扫描被@Service注解修饰的服务，初始化后保存在本地(都是单例的)，并把服务发布到nacos中。
 
    如果项目用到了spring,并且服务也被注册到了spring容器中,推荐在spring启动类上加入@Import(com.lzp.zprpc.common.util.SpringUtil.class)。
    先启动spring容器然后再启动rpc服务。这样在发布服务时，会先到spring容器中去找，如果spring容器中有服务实例，就会用spring中的。如果没有就会自己初始化一个。
   
    如果集群部署的话，建议同一个服务发布的个数为2的整次方，这样客户端在负载均衡时性能能更高
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
    和发布服务时一样,如果自己项目打成的包需要用特殊类加载器才能加载,则需要在获取代理对象的方法上加个类加载器参数。
    ServiceFactory.getServiceBean(serviceId,interfaceCls,loader);或ServiceFactory.getServiceBean(serviceId,interfaceCls,timeOut,loader);
 ### 三、Demo 
    源码中提供了demo，在rpc-demo工程下，包含了服务提供方工程和服务消费方工程，代码拉下来编译后直接就能跑，配置一下nacosIpList，
    先启动服务提供方再启动服务消费方就能看到结果。

## 主要实现原理：
    和其他主流rpc框架类似，服务提供方把服务注册到注册中心，服务消费方到注册中心找到相应服务，然后建立连接发起rpc调用。
    但是为了性能，这个rpc框架做了很多优化。
    1、不是每次rpc都会重新建立连接，实现了连接池机制，并且连接池里的连接是可以共用的，并不是取出来就少一个，然后放进去就多一个，
    是一个固定数量的连接池，数量可以通过配置文件配置。连接池里的连接也不是一次全建满，而是在客户端从注册中心中找到相应的实例后，
    从连接池中拿，发现连接池数量没满，就会初始化一个连接，放进连接池并返回。连接池中的连接会有心跳保活机制，当连接不可用了，会断
    掉这个连接发起新的连接并加入到连接池中。这其中的技术细节就涉及到多线程并发的问题，和单例模式有点类似，比如可见性、半初始化等。
    2、不是每次rpc调用都会阻塞一个连接。像http1.0，每次http请求都会新建连接，请求响应结束后就关闭连接，这期间这个连接只服务这一
    个http请求，http1.1虽然有keepalive和Pipeling机制，但是同一时间，连接只能服务一个http请求的，下个http请求要等待上个http请求
    返回结果后才会发出。而我这个连接机制是同一时间，多个rpc请求可以共用一个连接，没有任何阻塞的。
    
    举个例子，有A、B两个工程里面各包含了一组服务，分别部署在不同的机器上。A工程里的a方法会调用B工程里的e方法，b方法调用f方法，c方法调用g方法。
![example](https://gitee.com/zeping-lu/pngs-for-readme/raw/master/readme0.png)

    调用过程中他们都会从池中找出A工程和B工程的连接，然后通过这个连接进行RPC调用，这个过程是几乎是无序无阻塞的，a、b、c方法只要发出调用指令，然后
    等着拿到rpc结果就行。
    由于两个实例（ip+port）之间的所有服务通讯都是通过固定数量的连接(默认一个)，充分利用了每个连接，当有大量服务时，rpc调用所
    占有的cpu、内存、客户端的端口数、服务端的文件句柄数等资源会少很多。
    3、发起rpc时，不是每次都去nacos中根据serviceid找实例。只有第一次会去nacos中找，找出实例会缓存到本地，然后添加nacos事件监听，
    当有事件会刷新本地实例缓存。
    4、客户端代理对象是单例的，只有第一次获取服务的时候会初始化，之后会保存在容器中。
    5、服务端服务对象实例也是单例的，启动服务后会保存在容器中。如果项目用到了spring，会先去spring容器种找，找不到才自己初始化。
    6、通过自定义协议解决粘包拆包，序列化用的protostuff
    

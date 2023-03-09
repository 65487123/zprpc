# zprpc
中文|[English](https://github.com/65487123/zprpc/blob/master/README-EN.md)
# 功能介绍
一个高性能rpc框架，暂时只支持nacos(默认)和redis做注册中心。

# 项目整体架构
![architecture](https://github.com/65487123/zprpc/raw/master/architecture.png)
# 特点（相比其他一些主流rpc框架)：
* 配置简单，上手容易，非常轻量，占用资源小。
* 适用场景丰富：启动过程不依赖任何组件，比如tomcat、spring(有没有这些组件，用法都一样)。
支持OSGI环境(基于OSGI的工程打成的包是bundle而不是jar,如果依赖其他框架，框架很有可能找不到bundle中的类)。
* 高性能：与市面上常见的基于TCP协议的java高性能rpc框架简单对比测试了下,目前没发现有比这个性能高的。
* 支持在不关停JVM的情况下手动关停rpc服务,关停服务后,启动时注册的所有服务实例会在注册中心中自动被移除,rpcServer监听的端口会释放(如果有正在执行的任务,会等待指定时间)。  
关停后也可以再次启动服务。

# K8S环境下是否还有必要用这个rpc框架
K8S也提供了类似的功能(访问service的clusterIp的某个端口，k8s会自己帮忙负载均衡到service对应的所有存活pod中的其中一个的某个端口上)，所以K8S环境下也可以基于K8S自带的功能来实现rpc调用(如果觉得功能不够，可以在这基础上定制其他功能)。  

下面是这个rpc框架和K8S已提供的类似功能的一些区别。
## 和K8S自带的类似功能的区别
* 这个rpc框架中的服务粒度更细：这个rpc中，一个服务对应一个java中的接口(服务下挂着的每个实例对应相应java进程中的单例对象实例)   
而K8S中，service对应的是一组相同功能的pod，每个pod中跑着的是完成这个功能的一组容器(可以是docker也可以是其他)
* 这个性能更好：虽然现在K8S提供的rpc调用转发性能也不差(基于iptables实现),但个人感觉基于他提供的这个功能来实现服务间调用，只能是短连接,他把底层的路由规则对外屏蔽了，我们外面只能看到一个
clusterip,每次rpc调用时不知道他对应的真正pod是哪个，如果把他里面的pod ip都找出来建长连接并且自己做负载均衡，那也就没必要用这东西了。也就是说，K8S封装了底层的太多细节，很难在这基础上做太多性能上的优化.
* 用K8S来实现rpc,部署时要多做一些工作:比如必须要把每个服务都打成service，并且要配置service暴露的端口，而用这个rpc框架，不需要做这些配置，跑在pod中就行了(一个k8s集群中不同pod之间的网络是互通的，直接能用内部ip访问)。
* 这个需要额外部署注册中心,而K8S不需要：K8S他的服务注册和发现是基于etcd的，本身就自带的，而这个rpc框架目前没实现etcd作为注册中心，可能需要额外装一个注册中心。   

总结：如果应用是部署在K8S上的，可以用这个rpc框架，也可以基于K8S已有的功能自己实现rpc功能。具体选哪种方案，得根据自己的需求及喜好。

              
# 使用方法：
[nacos做注册中心](https://github.com/65487123/zprpc/blob/master/nacos.md) 

[redis做注册中心](https://github.com/65487123/zprpc/blob/master/redis.md)


## 主要实现原理：
和其他主流rpc框架类似，服务提供方把服务注册到注册中心，服务消费方到注册中心找到相应服务的所有实例，然后根据负载均衡策略找到某个实例，建立TCP连接发起rpc调用。但是为了性能，这个rpc框架做了很多优化。  

1、不是每次rpc都会重新建立连接,实现了连接池机制,并且连接池里的连接是可以共用的,并不是取出来就少一个，然后放进去就多一个,是一个有着固定数量连接的容器(说白了就是个List),连接数量可以通过配置文件配置,默认一个。连接池里的连接也不是一次全建满,而是在客户端从注册中心中找到相应的实例后,从连接池中拿,发现连接池数量没满，就会初始化一个连接，放进连接池并返回。这其中的技术细节就涉及到多线程并发的问题，比如可见性、半初始化等。连接池中的连接会有心跳保活机制,当连接不可用了,会把这个连接从池中移除。  

2、不是每次rpc调用都会阻塞一个连接。像http1.0,每次http请求都会新建连接,请求响应结束后就关闭连接，这期间这个连接只服务这一个http请求,http1.1虽然有keepalive和Pipeling机制,但是由于有序接收响应,  会有线头阻塞问题(没收到队头请求的响应,连接就一只阻塞在那了,后续的响应只能等着)。(http2虽说没上述问题,并且内容以二进制编码,但是它还是http协议,http协议里有那么多没用的字段就注定性能不会很高。http3据说性能更高，但它都不是基于TCP的，就不作比较了。)  
而我这个连接机制是同一时间,多个rpc请求可以共用一个连接,几乎没有任何阻塞的。  
举个例子，有A、B两个工程里面各包含了一组服务，分别部署在不同的机器上。A工程里的a方法会调用B工程里的e方法，b方法调用f方法，c方法调用g方法。
![example](https://gitee.com/zeping-lu/pngs-for-readme/raw/master/readme0.png)  
调用过程中他们都会从池中找出A工程和B工程的连接,然后通过这个连接进行RPC调用,这个过程是几乎是无序无阻塞的,a、b、c方法只要发出调用指令,然后
等着拿到rpc结果就行。(高性能rpc框架大致原理都差不多,所以代码实现细节也是决定性能差异的重要因素,比如线程间通信用锁还是park()/unpark()、请求id如何设置、方法序列化是整个方法Class序列化还是是只序列化方法名字字符串......)  
由于两个实例（ip+port）之间的所有服务通讯都是通过固定数量的连接(默认一个),充分利用了每个连接，当有大量服务时,rpc调用所
占有的cpu、内存、客户端的端口数、服务端的文件句柄数等资源会少很多。  

3、发起rpc时,不是每次都去注册中心根据serviceid找实例。只有第一次会去注册中心中找,找出实例会缓存到本地,注册中心的服务实例发生变动会
刷新本地实例缓存。  

4、客户端代理对象是单例的,只有第一次获取服务的时候会初始化,之后会保存在容器中。  

5、服务端服务对象实例也是单例的,启动服务后会保存在容器中。如果项目用到了spring,会先去spring容器种找,找不到才自己初始化。  

6、通过自定义协议解决粘包拆包,序列化用的protostuff。    
 ......
    

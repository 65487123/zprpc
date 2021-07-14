# zprpc
[中文](https://github.com/65487123/zprpc/blob/master/README.md)|English
# Introduction
    A high-performance rpc framework, temporarily only supports nacos as the registry.
    
#  Architecture
![architecture](https://github.com/65487123/zprpc/raw/master/architecture.png)
# Features：
    1、Simple configuration and easy to use.
    2、The application scenarios are rich
    The startup process does not depend on any components, such as tomcat, spring (with or without these components, the usage is the same)
    Support the OSGI environment (OSGI-based projects are packaged in bundles instead of jars. If you rely on other frameworks, the 
    framework may not find the classes in the  bundle).
    3、High performance: network communication is based on netty, protostuff for serialization. The connection pool 
	   mechanism is realized, and multiple RPC calls can share a connection at the same time (non-blocking), 
	   singleton proxy object, code details pay attention to performance.                  
# 	How to use：
### Environment setup(If the environment has been set up, you can skip)
    1、Pull the code to the local, execute the maven install command in the project root directory: mvn clean install
    2、Upload to Nexus (can be ignored, if you do, you don't need to perform step 1 in the follow-up, just rely on the maven coordinates)
    3、Set up the nacos environment: find the nacos source code on github, pull it locally, compile and start.（If there is already a nacos environment, skip)
     For those who are not familiar with the specific operation, please refer to the nacos official website 
   [nacos official website](https://nacos.io/zh-cn/docs/quick-start.html)
### Create project, import dependencies, write configuration and code
##### define public interfaces
    1、Create a new project and define public interfaces for service providers and service consumers to rely on
##### producer
    1、Create a service provider project, rely on the project that provides the interface, and import the maven dependency
    <dependency>
         <groupId>com.lzp.zprpc</groupId>
         <artifactId>rpc-artifacts</artifactId>
         <version>1.0</version>
    </dependency>
    2、Create an interface implementation class, implement specific methods of the interface, and add com.lzp.annotation.Service 
	annotation to the interface implementation class;
    The annotation has two parameters,the id of the service and the fully qualified name of the interface. 
	The id of the service must be unique
    Example：@Service(id = "serviceImpl", interfaceValue = "xxx.xxx.xxx.Service")
    3、Add the configuration file: zprpc.properties under the resources package, and add configuration items. Two of them are mandatory
    (1)The path of the package to be scanned: basePack. Example: basePack=zprpc.demo.producer
    (2)nacos ip: nacosIpList. Example: nacosIpList=192.168.0.101:8848
    In addition to configuring the address of the registry to be published through the configuration file, it can also be set through environment variables. The priority of         environment variables is greater than the configuration file.
    RPC_REGISTRY = xxx.xxx.xxx.xxx:8848
    Example: RPC_REGISTRY = 10.240.70.180:8848,10.240.70.173:8848,10.240.70.166:8848
    4、Start the service provider through code:
    Server.startRpcServer(ip,port);
    or Server.startRpcServer(port);
    or Server.startRpcServer();
    No ip is written, the default is the local ip.Neither ip nor port is written, the default is the local ip plus random available port
    
    In general, the classes or configuration files written by yourself are under the classpath, and the service can be published normally through the above startup           	     method.But if the class or configuration file in your own project needs a special class loader to load (customized class loader, for example, the project is based on     	       the OSGI framework, load different bundles A different class loader is required), you need to add a class loader parameter to the startup service method:
     Server.startRpcServer(ip,port,classLoader); or Server.startRpcServer(port,classLoader); or Server.startRpcServer(classLoader);
   
    After the service provider is started, it scans the services modified by the @Service annotation, saves them locally (all singletons) 
	after initialization, and publishes the services to nacos.
    
    If the project uses spring and the service is also registered in the spring container, it is recommended to add @Import(com.lzp.zprpc.common.util.SpringUtil.class) 
    to the spring boot class.Start the spring container first and then start the rpc service.
    In this way, when publishing a service, you will first find it in the spring container. If there is a service instance in the spring 
	container, it will use the spring. If not, it will initialize one itself.
 ##### consumer  
    1、Create service consumer projects, rely on projects that provide interfaces, and import dependencies
    <dependency>
         <groupId>com.lzp.zprpc</groupId>
         <artifactId>rpc-artifacts</artifactId>
         <version>1.0</version>
    </dependency>
    2、To write a configuration file, one item must be written:
    nacos ip: nacosIpList. Example: nacosIpList=192.168.0.101:8848
    You can also configure the number of connections to the connection pool for each instance.
    Example: connetionPoolSize: 2
    Without configuration, the number in the default connection pool is one. That is, the consumer communicates with all services in a 
    certain service instance through this connection, but there will be no blockage.
    It is recommended not to configure the number of connections in the connection pool and use the default connection pool of a single 
    connection. Because the client opens just one Reactor, that is, there is only one thread to serve all connections, and multiple 
    connections do not make much sense
    3.Get the proxy object, through the proxy object you can initiate a remote call, just like calling a local method
    com.lzp.zprpc.client.nacos.ServiceFactory.getServiceBean(String serviceId,Class interfaceCls);
    serviceId is the unique id of the service, and interfaceCls is the Class object of the interface. Return an instance and force it to be an 
	interface type.
    Can also
    ServiceFactory.getServiceBean(String serviceId,Class interfaceCls,int timeOut);
    To obtain the proxy object, remote calls through this object will have a timeout limit, and a timeout exception will be thrown if no result 
	is returned after the specified number of seconds.
    Just like when publishing a service, if the package of your own project needs to be loaded with a special class loader, you need to add a class loader parameter to               the method of obtaining the proxy object.
     ServiceFactory.getServiceBean(serviceId,interfaceCls,loader); or ServiceFactory.getServiceBean(serviceId,interfaceCls,timeOut,loader);
### Demo 
    The demo is provided in the source code. Under the rpc-demo project, the service provider project and the service consumer project are included. 
    After the code is pulled down and compiled, it can be run directly, configure nacosIpList,Start the service provider first and then start the service 
    consumer to see the result.
#### Main realization principle:
    Similar to other mainstream RPCs, the service provider registers the service in the registry, and the service consumer finds the corresponding 
    service in the registry, and then establishes a connection to initiate an rpc call. But for performance, this rpc framework has made many optimizations.
    1、Not every time rpc will re-establish the connection, the connection pool mechanism is implemented, and the connections in the connection pool 
       can be shared, not just take out one less, and then put in one more.It is a fixed number of connection pools, the number can be configured through 
       the configuration file. The connection in the connection pool is not fully established at one time, but after the client finds the corresponding 
       instance in the registry,Take it from the connection pool and find that the number of connection pools is not full, it will initialize a connection, 
       put it into the connection pool and return. The connection in the connection pool will have a heartbeat keep-alive mechanism. When the connection is 
       unavailable, it will be disconnected.Drop this connection to initiate a new connection and add it to the connection pool. The technical details 
       involve the issue of multi-thread concurrency, which is similar to the singleton mode, such as visibility, semi-initialization, etc.
    2、Not every rpc call will block a connection. Like http1.0, every http request creates a new connection, and then closes the connection after the 
	request is initiated. During this time, this connection only serves this one A http request. Although http1.1 has keepalive and pipeling mechanisms, 
	due to the orderly reception of responses, there will be a line header blocking problem (if the response to the head of the queue request is not received,
	the connection is blocked, and the subsequent response can only be waited for). (Although http2.0 does not have the above problems, and the content is 
	encoded in binary, it is still the http protocol. There are so many useless fields in the http protocol, and the performance is destined to not be very high) 
	And my connection mechanism is at the same time, multiple RPC requests can share a connection, almost without any blocking.
	For example, there are two projects A and B each containing a set of services, which are deployed on different machines. Method a in project A will 
	call method e in project B, method b will call method f, and method c will call method g.
![example](https://gitee.com/zeping-lu/pngs-for-readme/raw/master/readme0.png)
 
        During the calling process, they will find the connection between Project A and Project B from the pool, and then make RPC calls through this connection. 
	This process is almost disorderly and non-blocking. A, b, and c methods only need to issue a call instruction, and then Just wait to get the rpc result.
        Since all service communication between the two instances (ip+port) is through a fixed number of connections (the default is one), every connection is fully 
	utilized, When there are a large number of services, rpc calls occupied CPU、memory、the number of client ports、the number of file 
	handles on the server and other resources will be much less 
    3、When initiating rpc, it is not every time to find an instance in nacos based on serviceid. Only the first time I will go to nacos to find, find out 
	the instance will be cached locally, and then add nacos to monitor events, When there is an event, the cache of this instance will be refreshed.
    4、The client proxy object is a singleton, and only the first time it gets the service will be initialized, and then it will be stored in the container.
    5. The server-side service object instance is also singleton, and will be stored in the container after the service is started. If the project uses spring, 
    it will first go to the spring container to find it, and then initialize it by itself.
    6、Solve sticky package and unpack through custom protocol, Protostuff for serialization

    

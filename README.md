### SSM框架实现高并发秒杀API学习笔记

来自[yijun zhang](https://www.imooc.com/u/2145618/courses?sort=publish)老师的《Java高并发秒杀API》系列课程



#### maven命令创建web骨架项目

mvn archetype:generate -DgroupId=org.seckill -DartifactId=seckill -DarchetypeArtifactId=maven-archetype-webapp

 groupId和artifactId用来标注项目坐标，项目名为org.seckill；archetypeArtifactId指明使用maven的webapp原型来创建项目。

创建成功后选定pom文件导入IDEA。

创建需要的文件夹，并选定其类型，如源文件夹java，资源文件夹resources。

其中web.xml文件的使用的是Servlet2.3版本，默认jsp的el表达式不工作，需要切换为更高的版本。进入~\apache-tomcat-9.0.14\webapps\examples\WEB-INF，打开其中的web.xml文件，将其<web-app>标签内容复制过来，替换项目文件中的<web-app>标签。

 pom文件中junit默认是3.8版本，3.0版本默认使用编程方式，4.0使用注解方式来运行，需替换为4.0版本。

#### 导入相关依赖

1.日志：使用slf4j + logback

slf4j-api、logback-core、logback-classic

2.数据库相关依赖，使用mybatis、mysql，连接池使用druid

mysql-connector-java、druid、mybatis、mybatis-spring

3.Servlet web相关依赖

standard、jackson-databind、javax.servlet-api

4.spring依赖

核心依赖：spring-core、spring-beans、spring-context

dao层相关依赖：spring-jdbc、spring-tx、jstl

web相关依赖：spring-web、spring-webmvc

test相关依赖：spring-test



**数据库创建**：创建数据库，创建秒杀库存表，并初始化一些信息，创建秒杀成功明细表（schema.sql）



#### Dao层相关

创建entity层，放置实体类。对应数据库表。

创建dao层，dao层相关接口，定义增删改查方法



##### mybatis配置

创建mybatis配置文件mybatis-config，进入[mybatis官网](http://www.mybatis.org/mybatis-3/zh/getting-started.html)查看示例，配置全局属性，例：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <!--配置全局属性-->
    <settings>
        <!--使用jdbc的getGeneratedKeys 获取数据库自增主键值-->
        <setting name="useGeneratedKeys" value="true"/>
        <!--使用列别名替换列名 默认：true-->
        <setting name="useColumnLabel" value="true"/>
        <!--开启驼峰命名转换：Table(create_time) -> Entity(createTime)-->
        <setting name="mapUnderscoreToCamelCase" value="true"/>
    </settings>
</configuration>
```



在resources文件夹下创建mapper层，其下创建映射dao层的sql配置文件,[官网](http://www.mybatis.org/mybatis-3/zh/configuration.html)，例：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.seckill.dao.SeckillDao">
    <!--目的：为dao接口方法提供sql语句配置-->

    <!--xml中不允许出现<=，使用<![CDATA[ ]]>标签，<![CDATA[<=]]>-->
    <update id="reduceNumber">
        update seckill
        set number = number -1
        where seckill_id = #{seckillId}
        and start_time <![CDATA[<=]]> #{killTime}
        and end_time >= #{killTime}
        and number >0;
    </update>

    <select id="queryById" resultType="Seckill" parameterType="long">
          select seckill_id,name,number,start_time,end_time,create_time
          from seckill
          where seckill_id = #{seckillId}
    </select>
</mapper>
```



创建jdbc配置文件jdbc.properties，例：

```properties
driver=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://localhost:3306/seckill?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
user=root
password=123456
```



##### mybatis整合spring

mybatis整合spring，在resources文件夹下创建spring层，放置与spring相关配置。创建spring-dao.xml配置文件，在其中配置整合mybatis过程。例：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
    <!--  配置整合mybatis过程 -->
    <!--  1、配置数据库相关参数 properties的属性：${url}-->
    <context:property-placeholder location="classpath:jdbc.properties"/>

    <!--2:数据库连接池-->
    <bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
        <!--配置连接池属性-->
        <property name="driverClassName" value="${driver}"/>
        <property name="url" value="${url}"/>
        <property name="username" value="${user}"/>
        <property name="password" value="${password}"/>
        
        <property name="initialSize" value="10"/>
        <property name="maxActive" value="30"/>
        <property name="minIdle" value="10"/>
        <!--关闭连接后不自动commit，默认false-->
        <property name="defaultAutoCommit" value="false"/>

        <property name="maxWait" value="1000"/>

        <property name="timeBetweenConnectErrorMillis" value="60000"/>
        <property name="minEvictableIdleTimeMillis" value="300000" />
    </bean>

    <!--约定大于配置-->
    <!--3:配置SqlSessionFactory对象-->
    <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <!--注入数据库连接池-->
        <property name="dataSource" ref="dataSource"/>
        <!--配置Mybatis全局配置文件-->
        <property name="configLocation" value="classpath:mybatis-config.xml"/>
        <!--扫描entity包 使用别名 org.seckill.entity.Seckill->Seckill-->
        <property name="typeAliasesPackage" value="org.seckill.entity"/>
        <!--扫描sql配置文件:mapper需要的xml文件-->
        <property name="mapperLocations" value="classpath:mapper/*.xml"/>
    </bean>

    <!--4:配置扫描Dao接口包,动态实现Dao接口,注入到Spring容器中-->
    <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
        <!--注入sqlSessionFactory-->
        <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory"/>
        <!--给出扫描Dao接口包-->
        <property name="basePackage" value="org.seckill.dao"/>
    </bean>

</beans>
```



单元测试

[错误1](https://www.imooc.com/qadetail/296113)  、[错误2](https://www.imooc.com/qadetail/220050)



#### Service层相关

创建dto层，数据传输层，例如封装执行秒杀后的结果、封装json数据、暴露秒杀地址

创建exception层，自定义继承自RuntimeException的异常，例：

```java
package org.seckill.exception;

// 秒杀相关业务异常
public class SeckillException extends RuntimeException{
    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

可继续创建继承此异常类的详细异常，如重复异常、秒杀关闭异常，同上。

创建enums层，枚举返回信息

创建service，实现业务逻辑，接口与实现



##### 使用spring托管Service

使用spring托管Service依赖配置，创建spring-service.xml配置文件，在其中配置，例：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context" xmlns:tx="http://www.springframework.org/schema/tx"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx.xsd">

    <!--扫描service包下所有使用注解的类型-->
    <context:component-scan base-package="org.seckill.service"/>

    <!-- 配置事务管理器 -->
    <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <!-- 注入数据库连接池 -->
        <property name="dataSource" ref="dataSource"/>
    </bean>

    <!--配置基于注解的声明式事务
        默认使用注解来管理事务行为
    -->
    <tx:annotation-driven transaction-manager="transactionManager"/>
</beans>
```



##### 使用日志

使用到了日志，[官网参考](https://logback.qos.ch/manual/configuration.html)创建logback.xml配置文件进行配置，最简单例：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<configuration debug="true">

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <!-- encoders are  by default assigned the type
             ch.qos.logback.classic.encoder.PatternLayoutEncoder -->
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="debug">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```



单元测试



#### Controller层相关

##### 整合配置SpringMVC框架

创建spring-web.xml配置文件，在其中配置Spring MVC，例：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:mvc="http://www.springframework.org/schema/mvc"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
    <!--配置Spring-MVC-->
    <!-- 1、开启springMVC注解模式-->
    <!-- 简化配置：
        (1) 自动注册DefaultAnnotationHandlerMapping,AnnotationMethodHandlerAdapter
        (2) 提供了一系列功能：数据绑定/数字和日期的format@NumberFormat @DateTimeFormat,
            xml 和 json默认读写支持
    -->
    <mvc:annotation-driven/>

    <!--servlet-mapping 映射路径：/ -->

    <!--2、静态资源配置 默认servlet配置
        1）加入对静态资源的处理 js gif png
        2）允许使用“/”做整体映射
    -->
    <mvc:default-servlet-handler/>

    <!-- 3、配置jsp 显示viewResolver-->
    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="viewClass" value="org.springframework.web.servlet.view.JstlView"/>
        <property name="prefix" value="/WEB-INF/jsp/"/>
        <property name="suffix" value=".jsp"/>
    </bean>

    <!--4、扫描web相关的bean-->
    <context:component-scan base-package="org.seckill.web"/>
</beans>
```



创建web层，即控制层，实现Restful接口设计

在web.xml文件中配置DispatcherServlet，例：

```xml
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
                      http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0"
         metadata-complete="true">
    <!--配置DispatcherServlet-->
    <servlet>
        <servlet-name>seckill-dispatcher</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <!-- 配置springMVC需要加载的配置文件
                spring-dao.xml,spring-service.xml,spring-web.xml
        -->
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>classpath:spring/spring-*.xml</param-value>
        </init-param>
    </servlet>
    <servlet-mapping>
        <servlet-name>seckill-dispatcher</servlet-name>
        <!-- '/'默认匹配所有请求-->
        <url-pattern>/</url-pattern>
    </servlet-mapping>
</web-app>

```



使用BootStrap与jQuery开发页面结构，再使用JS实现交互逻辑

页面测试：成功



#### 高并发优化

##### 优化分析

##### CDN

CDN（内容分发网络）是加速用户获取数据的系统，部署在离用户最近的网络节点上，命中CDN不需要访问后端服务器。

系统详情页部署到CDN节点，CDN对详情页静态化，详情页中有很多获取静态资源的请求css，js等，这些请求的资源也会部署到CDN，**访问详情页不用访问系统**，即获取不到系统时间，所以做一个请求获取当前服务器的系统时间很有必要。

通过上面分析知访问详情页不用访问系统，java访问一次内存（Cacheline）大约10ns，即1秒钟大约可做1亿次，所以**获取系统时间不用优化**。

秒杀地址接口返回数据会变化，无法使用CDN缓存，适合服务器缓存：Redis等，服务器1秒可抗10wQPS,redis集群后可抗百万级QPS（QPS为每秒查询率），一次性维护成本低。可优化。

![redis](https://github.com/xiruitao/image-music/blob/images/redis.png?raw=true)



其他方案：

![NoSQLMQ](https://github.com/xiruitao/image-music/blob/images/RedisMQ.png?raw=true)

优点：可抗很高的并发

缺点：运维成本和稳定型：NoSQL、MQ等为分布式服务，稳定性不足

​	    开发成本：数据一致性，回滚方案等，需要人工处理

 	    幂等性难保证：重复秒杀问题

​	    不适合新手的架构



为什么不使用MySQL优化？

MySQL真的低效？

测试：同一个id执行update减库存压力测试

![update压力测试](https://github.com/xiruitao/image-music/blob/images/update%E5%8E%8B%E5%8A%9B%E6%B5%8B%E8%AF%95.png?raw=true)

约4wQPS



**java控制事务行为分析**

![shiwu](https://github.com/xiruitao/image-music/blob/images/java%E4%BA%8B%E5%8A%A1.png?raw=true)

**瓶颈分析**

![fenxi](https://github.com/xiruitao/image-music/blob/images/%E7%93%B6%E9%A2%88%E5%88%86%E6%9E%901.png?raw=true)

​					Java GC（Garbage Collection，垃圾收集，垃圾回收）机制

**分析结果**：不是MySQL、Java慢。原因为java客户端执行SQL语句，等待执行结果，再做判断，再去执行，而java和数据库通信之间会有网络延迟或者GC，这些时间也要加载执行事务中，而同一行事务为串行化。

行级锁在commit之后释放，所以优化方向是减少行级锁持有时间

**延迟分析**

![同城机房网络](https://github.com/xiruitao/image-music/blob/images/%E5%90%8C%E5%9F%8E%E6%9C%BA%E6%88%BF.png?raw=true)

![异地机房网络](https://github.com/xiruitao/image-music/blob/images/%E5%BC%82%E5%9C%B0%E6%9C%BA%E6%88%BF.png?raw=true)



**如何判断Update更新库存成功？**

两个条件：1、Update自身没报错；2、客户端确认Update影响记录数

优化思路：

**把客户端逻辑放到MySQL服务端，避免网络延迟和GC影响**



**如何放到MySQL服务端？**

两种解决方案：

——定制SQL方案：update /* + [auto_commit] */ ,需要修改MySQL源码

执行update后自动做回滚，update记录数为1时commit，0则rollback，避免客户端与MySQL服务端之间的网络延迟和GC影响

——**使用存储过程：整个事务在MySQL端完成**



**优化总结**：

前端控制：暴露接口（将动、静态数据及核心功能点分离）、按钮防重复

动静态数据分离：CDN缓存、后端缓存（Redis、MemCache）

事务竞争优化：减少事务锁时间



##### Redis后端缓存优化

[Redis安装](https://blog.csdn.net/ring300/article/details/80434655?utm_source=blogkpcl0)（64位Windows下）

优化地址暴露接口

导入redis客户端jedis及序列号工具protostuff

dao为放置数据库及其他存储类的包，在dao包下创建cache层，cache层下放RedisDao类，缓存在redis中为二进制数组，若redis中存在缓存，获取二进制数组byte[]，进行反序列化，将数据放入创建的空对象。若不存在缓存，则将对象序列化转换为byte[]放入redis。

改进秒杀实现方法中获取数据，先访问redis，若无数据，则访问数据库，并将数据放入redis。

在spring-dao.xml文件中配置RedisDao，例

```xml
<!--RedisDao-->
<bean id="redisDao" class="org.seckill.dao.cache.RedisDao">
    <!--实际配置应放入配置文件-->
    <constructor-arg index="0" value="localhost"/>
    <constructor-arg index="1" value="6379"/>
</bean>
```



此缓存优化一致性维护建立在超时的基础上，因为秒杀对象在正常情况下不会改变，若需要改动，废弃新建。



##### 事务执行优化

update 减库存 【rowLock】 <——网络延迟、GC——> insert 购买明细 <——网络延迟、GC——>

commit/rollback【freeLock】

**简单优化**——减少了行级锁持有时间，使MySQL获得更高的QPS

insert 购买明细 <——网络延迟、GC——> update 减库存【rowLock】 <——网络延迟、GC——>

commit/rollback【freeLock】



**深度优化**——将事务SQL在MySQL端执行（存储过程）

在数据库创建存储过程（seckill.sql）

然后在SeckillDao接口中新建使用存储过程执行秒杀的方法，此方法传入参数为Map类型（在service实现方法中需要将结果放入参数告诉MySQL，该操作使用了org.apache.commons.collections的MapUtils方法，需在pom文件中引入）。然后在其对应配置文件中使用mybatis调用存储过程。再在秒杀业务接口中新建通过存储过程执行秒杀操作的方法，在其实现类中实现该方法，在该方法中调用dao接口中的方法。测试1成功。在controller中改用该方法，网页测试，成功。



系统可能用到哪些服务？

CDN、 WebServer：Nginx + Jetty、 Redis、 MySQL

大型系统部署架构

![jiagou](https://github.com/xiruitao/image-music/blob/images/%E6%9E%B6%E6%9E%84.png?raw=true)

开发：前端 + 后端	测试（迭代、修改、压力测试等）	DBA（存储过程、分库分表等）	运维（负责机器监控、Nginx配置等）



—关于使用current_timestamp时间与北京时间相差8小时解决办法

https://blog.csdn.net/uzbekistan/article/details/80591367

—关于Tomcat日志输出中文乱码，网上几种方法无效，待解决
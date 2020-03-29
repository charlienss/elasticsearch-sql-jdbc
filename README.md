## ElasticSearch-Sql + Mybatis + Druid + SpringBoot 实现ES的综合操作
#### 1.安装ElasticSearch6.5.1

​		解压相关的ElasticSearch6.5.1的tar包到目录下,如果我们需要使用JDBC来连接的话是需要到白金版以上的,分享出本人将基础版破解为白金版的包给大家.

##### 本人已将破解包放项目的file分支,拉取相应分支即可获得,项目file分支地址如下:

```java
欢迎start:
https://github.com/fengcharly/elasticsearch-sql-jdbc/tree/file
```

##### 关于破解:

​		由于我们的包替换了相应的class文件,所以直接解压即可,然后把license放到相应的位置并执行以下的命令,看到返回的是Active就代表已经破解完成:

```java
curl -XPOST  -H 'Content-Type:application/json'  -d @license-platinum.json  'http://192.168.142.128:9200/_xpack/license?acknowledge=true&pretty'
```

##### 映射关系如下:

| Mysql               | Elasticsearch         |
| ------------------- | --------------------- |
| Database            | Index                 |
| Table               | Type                  |
| Row                 | Document              |
| Column              | Field                 |
| Schema              | Mapping               |
| Index               | Everything is indexed |
| SQL                 | Query DSL             |
| Select * from table | Get http://           |
| Update table set    | Put http://           |

#### 2.索引的操作命令

##### ①建表(es的索引):

```java
[PUT] http://192.168.142.128:9200/test

请求体:
{
    "mappings": {
        "people_test": {
            "dynamic": "strict",
            "properties": {
                "id": {
                    "type": "long"
                },
                "name": {
                    "type": "text"
                },
                "age": {
                    "type": "integer"
                },
                "createTime": {
                    "type": "text"
                }
            }
        }
    }
}
```

##### ②增加数据

```java
[POST] http://192.168.142.128:9200/test/people_test/1

请求体:

{
    "id": "1",
    "name": "投桃报李",
    "age": "18",
    "createTime": "99885552"
}
```

##### ③删除索引

```java
[DELETE] http://192.168.142.128:9200/test
```

增加数据的时候报异常:

```java
{
    "error": {
        "root_cause": [
            {
                "type": "cluster_block_exception",
                "reason": "blocked by: [FORBIDDEN/12/index read-only / allow delete (api)];"
            }
        ],
        "type": "cluster_block_exception",
        "reason": "blocked by: [FORBIDDEN/12/index read-only / allow delete (api)];"
    },
    "status": 403
}
```

##### 解决方法1:

运行命令:

```java
curl -XPUT -H "Content-Type: application/json" http://192.168.142.128:9200/people_test/_settings -d '{"index.blocks.read_only_allow_delete": null}'
```

##### 解决方法2:

创建索引的时候指定:

```java
{
	"settings":{
		"index":{
			"number_of_shards":"2",
			"number_of_replicas":"1",
			"blocks.read_only_allow_delete":"false"
		}
	}
}
```

还有一种可能是当磁盘的使用率超过95%时，Elasticsearch为了防止节点耗尽磁盘空间，自动将索引设置为只读模式。

1、最简单也是最直接的是清理磁盘空间
2、更改elasticsearch.yml配置文件，在config/elasticsearch.yml中增加下面这句话

```shell
cluster.routing.allocation.disk.watermark.flood_stage: 99%
```

这是把控制洪水阶段水印设置为99％，你也可以自己设置其他百分比，默认是95%。
3、更改elasticsearch.yml配置文件，在config/elasticsearch.yml中增加下面这句话

```shell
cluster.routing.allocation.disk.threshold_enabled: false
```

默认为true。设置为false禁用磁盘分配决策程序。
上面无论哪一种方法修改之后，都需要重启elasticsearch，然后再把索引的read_only_allow_delete设置为false，采用一中的方法中的任意一种即可，更改后再查看索引的信息，如图，read_only_allow_delete配置没有了，表示以及设置成功了。



#### 3.相关问题与解决方案:

##### ①启动报内存不足等问题:

1、max file descriptors [4096] for elasticsearch process is too low, increase to at least [65536]

　　每个进程最大同时打开文件数太小，可通过下面2个命令查看当前数量

```shell
ulimit -Hn
ulimit -Sn
```

　　修改/etc/security/limits.conf文件，增加配置，用户退出后重新登录生效

```shell
*               soft    nofile          65536
*               hard    nofile          65536
```

2、max number of threads [3818] for user [es] is too low, increase to at least [4096]

　　问题同上，最大线程个数太低。修改配置文件/etc/security/limits.conf（和问题1是一个文件），增加配置

```shell
*               soft    nproc           4096
*               hard    nproc           4096
```

　　可通过命令查看

```shell
ulimit -Hu
ulimit -Su
```

3、max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]

　　修改/etc/sysctl.conf文件，增加配置vm.max_map_count=262144

```shell
vi /etc/sysctl.conf
sysctl -p
```

　　执行命令sysctl -p生效

 4、Exception in thread "main" java.nio.file.AccessDeniedException: /usr/local/elasticsearch/elasticsearch-6.2.2-1/config/jvm.options

　　elasticsearch用户没有该文件夹的权限，执行命令

```shell
chown -R es:es /usr/local/elasticsearch/
```

无法用root启动

```java
新建非root用户,如admin
useradd admin
chmod 777 -R elasticsearch
```

##### ②启动命令:

ElasticSearch的bin目录下面:

```java
启动命令有两种:
./elasticsearch     前台启动方式 会在下面打印日志
./elasticsearch -d  后台的启动方式
```

##### ③启动后需要升级为白金版,命令在上面查找

#### 4.使用SpringBoot连接ElasticSearch-sql+mybatis的操作:

##### ①新建SpringBoot项目

##### POM依赖如下:

```java
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.2.2.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.es</groupId>
    <artifactId>elastic-sql</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>elastic-sql</name>
    <description>Demo project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <java.version>1.8</java.version>
        <log4j2.version>2.8.2</log4j2.version>
        <slf4j.version>1.7.25</slf4j.version>
        <druid.version>1.1.10</druid.version>
        <spring.version>5.0.2.RELEASE</spring.version>
    </properties>

    <dependencies>
        <!-- elastic相关-->
        <dependency>
            <groupId>org.elasticsearch.plugin</groupId>
            <artifactId>x-pack-sql-jdbc</artifactId>
            <version>6.5.2</version>
        </dependency>


        <!-- ################## Database ################## -->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>1.3.2</version>
            <exclusions>
                <!--排除自带的logback依赖-->
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-logging</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.mybatis</groupId>
                    <artifactId>mybatis</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.mybatis</groupId>
                    <artifactId>mybatis-spring</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>5.1.39</version><!--$NO-MVN-MAN-VER$-->
        </dependency>
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis</artifactId>
            <version>3.4.5</version><!--$NO-MVN-MAN-VER$-->
        </dependency>
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis-spring</artifactId>
            <version>1.3.1</version><!--$NO-MVN-MAN-VER$-->
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid</artifactId>
            <version>${druid.version}</version><!--$NO-MVN-MAN-VER$-->
        </dependency>

        <dependency>
            <groupId>com.meizu</groupId>
            <artifactId>meizu-framework-datasource</artifactId>
            <version>1.2</version>
        </dependency>

        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.0</version>
        </dependency>

        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.32</version>
        </dependency>


        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
            <version>2.1.3.RELEASE</version>
        </dependency>

    </dependencies>

    <repositories>
        <repository>
            <id>elastic.co</id>
            <url>https://artifacts.elastic.co/maven</url>
        </repository>
    </repositories>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>

```

##### YML文件:

```java
spring:
  datasource:
    es:
      url: jdbc:es://http://192.168.142.128:9200
      driver-class-name: org.elasticsearch.xpack.sql.jdbc.jdbc.JdbcDriver
      mapperLocations: classpath*:/mybatis/*.xml
      configLocation: classpath:/config/elastic-mybatis.xml
```

##### loback.xml:

```java
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
	<!-- %d日期，%t线程名，%c类的全名，%p日志级别，%file文件名，%line行数，%m%n输出的信息 -->
	<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
		<encoder>
			<pattern>%d [%t] [%c] [%p] (%file:%line\)- %m%n</pattern>
			<charset>UTF-8</charset>
		</encoder>
	</appender>
<!--	<appender name="SYS_SERVER"-->
<!--		class="ch.qos.logback.core.rolling.RollingFileAppender">-->
<!--		<File>data/log/automarket-server.log</File>-->
<!--		<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">-->
<!--			<fileNamePattern>data/log/automarket-server.log.%d.%i</fileNamePattern>-->
<!--			<timeBasedFileNamingAndTriggeringPolicy-->
<!--				class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">-->
<!--				&lt;!&ndash; or whenever the file size reaches 64 MB &ndash;&gt;-->
<!--				<maxFileSize>64 MB</maxFileSize>-->
<!--			</timeBasedFileNamingAndTriggeringPolicy>-->
<!--			&lt;!&ndash; 保留天数 &ndash;&gt;-->
<!--			<maxHistory>7</maxHistory>-->
<!--		</rollingPolicy>-->
<!--		<encoder>-->
<!--			<pattern>-->
<!--				%d [%t] [%c] [%p] (%file:%line\)- %m%n-->
<!--			</pattern>-->
<!--			<charset>UTF-8</charset> &lt;!&ndash; 此处设置字符集 &ndash;&gt;-->
<!--		</encoder>-->
<!--	</appender>-->

	<appender name="SYS_SERVER" class="ch.qos.logback.core.rolling.RollingFileAppender">
		<File>/data/log/jetty/automarket-server.log</File>
		<append>true</append>
		<!--过滤器,只打INFO级别的日志-->
		<filter class="ch.qos.logback.classic.filter.LevelFilter">
			<level>INFO</level>
			<onMatch>ACCEPT</onMatch>
			<onMismatch>DENY</onMismatch>
		</filter>
		<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
			<fileNamePattern>/data/log/jetty/automarket-server.log.%d</fileNamePattern>
			<maxHistory>30</maxHistory>
		</rollingPolicy>

		<encoder charset="UTF-8">
			<pattern>[%d{yyyy-MM-dd HH:mm:ss.SSS}] %level [%thread] %file:%line - %msg%n</pattern>
			<charset>UTF-8</charset>
		</encoder>
	</appender>

	<appender name="SYS_ERROR" class="ch.qos.logback.core.rolling.RollingFileAppender">
		<File>/data/log/jetty/automarket-error.log</File>
		<append>true</append>
		<!--过滤器,只打ERROR级别的日志-->
		<filter class="ch.qos.logback.classic.filter.LevelFilter">
			<level>ERROR</level>
			<onMatch>ACCEPT</onMatch>
			<onMismatch>DENY</onMismatch>
		</filter>
		<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
			<fileNamePattern>/data/log/jetty/automarket-error.log.%d</fileNamePattern>
			<maxHistory>7</maxHistory>
		</rollingPolicy>

		<encoder charset="UTF-8">
			<pattern>[%d{yyyy-MM-dd HH:mm:ss.SSS}] %level [%thread] %file:%line - %msg%n</pattern>
			<charset>UTF-8</charset>
		</encoder>
	</appender>

	<root level="INFO">
		<appender-ref ref="STDOUT" />
		<appender-ref ref="SYS_SERVER" />
		<appender-ref ref="SYS_ERROR"/>
	</root>
	<logger name="com.wakedata.dss.automarket" level="DEBUG"/>
	<logger name="com.wakedata.dss.automarket" level="INFO"/>

</configuration>
```

##### elastic-mybatis.xml:

```java
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <settings>    <!-- 开启二级缓存 -->
        <setting name="cacheEnabled" value="true"/>

        <!-- 打印查询语句 -->
        <setting name="logImpl" value="STDOUT_LOGGING" />
        <setting name="useColumnLabel" value="false" />
    </settings>
</configuration>
```

##### EsDruidDataSourceConfig:

```java
package com.es.elasticsql.configuration;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;


/**
 * @program:
 * @description: es 数据源配置
 **/
@Configuration
@MapperScan(basePackages = {"com.es.elasticsql.mapper"}, sqlSessionFactoryRef = "esSqlSessionFactory")
public class EsDruidDataSourceConfig {

    @Value("${spring.datasource.es.configLocation}")
    private String configLocation;

    @Value("${spring.datasource.es.mapperLocations}")
    private String bigdataMapperLocations;

    @Value("${spring.datasource.es.url}")
    private String esUrl;

    @Bean(name = "esDataSource")
    public DataSource esDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("org.elasticsearch.xpack.sql.jdbc.jdbc.JdbcDriver");
        dataSource.setUrl(esUrl);
        return dataSource;
    }

    /**
     * SqlSessionFactory配置
     *
     * @return
     * @throws Exception
     */
    @Bean(name = "esSqlSessionFactory")
    public SqlSessionFactory bigdataSqlSessionFactory(@Qualifier("esDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        //配置mapper文件位置
        sqlSessionFactoryBean.setMapperLocations(resolver.getResources(bigdataMapperLocations));
        sqlSessionFactoryBean.setConfigLocation(resolver.getResource(configLocation));
        return sqlSessionFactoryBean.getObject();
    }
}
```

相关Git项目地址如下:

```java
https://github.com/fengcharly/elasticsearch-sql-jdbc
```

#### 5.部署Kibana

##### ①下载kibana插件

https://artifacts.elastic.co/downloads/kibana/kibana-6.2.3-linux-x86_64.tar.gz

```java
wget https://artifacts.elastic.co/downloads/kibana/kibana-6.2.3-linux-x86_64.tar.gz
```

##### ②解压到相应文件夹

```shell
tar -zxvf kibana-6.2.3-linux-x86_64.tar.gz
```

##### ③在这里要注意版本号的问题

1、kibana和elsticserch版本不能差别大，否则无法正常使用 比如 Kibana 6.x 和 Elasticsearch 2.x不能正常使用
2、运行比Kibana更高版本的Elasticsearch通常可以工作 例如Kibana 5.0和Elasticsearch 5.1
3、小版本差异会有一些警告出现，除非两者升级到相同的版本
这是官方给出的说明： https://www.elastic.co/guide/en/kibana/current/setup.html

##### ④启动Kibanan

```shell
./bin/kibana
```

默认是前台启动的方式,我们在访问如下的网址的时候发现并不能访问,可以看到日志中有这么一句:

```java
Server running at http://localhost:5601
```

外网是无法访问的，如果外网想访问，那需要修改一下server.host,我们放开端口

```java
vim /data/kibana-6.2.3-linux-x86_64/config/kibana.yml
```

放开server.host，并修改如下：

```java
server.port: 5601
server.host: 0.0.0.0
```

##### ⑤kibana后台启动

```java
nohup ./bin/kibana &
```

##### ⑥kibana目录结构

- bin： 二进制脚本，包括 kibana 启动 Kibana 服务和 kibana-plugin 安装插件。
- config： 配置文件，包括 kibana.yml 。
- data： Kibana 和其插件写入磁盘的数据文件位置。
- optimize： 编译过的源码。某些管理操作(如，插件安装)导致运行时重新编译源码。
- plugins： 插件文件位置。每一个插件都有一个单独的二级目录。

##### ⑦windows下安装启用文档

https://www.elastic.co/guide/en/kibana/current/windows.html

##### ⑧Kibana 配置文件 kibana.yml 简单说明

| 配置                                             | 说明                                                         |
| ------------------------------------------------ | ------------------------------------------------------------ |
| server.port :                                    | 默认值 : 5601 Kibana 由后端服务器提供服务。此设置指定要使用的端口。 |
| server.host :                                    | 默认值 : “localhost” 此设置指定后端服务器的主机。            |
| server.name :                                    | 默认值 : “your-hostname” 用于标识此 Kibana 实例的可读的显示名称。 |
| elasticsearch.url :                              | 默认值 : “http://localhost:9200” 要用于所有查询的 Elasticsearch 实例的 URL。 |
| elasticsearch.pingTimeout                        | 日常用的ping，默认值 : 值 elasticsearch.tribe.requestTimeout 设置以毫秒为单位的时间等待 Elasticsearch 对 PING 作出响应。 |
| elasticsearch.requestTimeout                     | 默认值 : 30000 等待来自后端或 Elasticsearch 的响应的时间（以毫秒为单位）。此值必须为正整数。 |
| elasticsearch.username 和 elasticsearch.password | 如果您的 Elasticsearch 受基本认证保护，这些设置提供 Kibana 服务器用于在启动时对 Kibana 索引执行维护的用户名和密码。您的 Kibana 用户仍需要使用通过 Kibana 服务器代理的 Elasticsearch 进行身份验证。 |

#### 6.集群部署

要配置集群，最简单的情况下，修改elasticsearch.yml,设置下面几个参数就可以了,注意开放9300端口：

```shell
transport.tcp.port: 9300
cluster.name: es-cluster
node.name: es-node-1
discovery.zen.ping.unicast.hosts: ["172.31.3.36","172.31.3.35"]
discovery.zen.minimum_master_nodes: 1
```

如果报已存在的节点id,是因为复制虚拟机时，elsticsearch时，将elsticsearch文件夹下的data文件夹一并复制了。而在前面测试时，data文件夹下已经产生了data数据，于是报上面的错误。

**解决办法：删除elsticsearch文件夹下的data文件夹下的节点数据**

```java
查看节点状态: /_cluster/health
如果是Green则代表配置正确
```

#### 7.使用logstash导入mysql数据到elasticSearch

Elasticsearch-jdbc工具包（废弃）,虽然是官方推荐的，但是已经几年不更新了。所以选择安装logstash-input-jdbc，首选 logstash-input-jdbc,logstash5.X开始，已经至少集成了logstash-input-jdbc插件。所以，你如果使用的是logstash5.X，可以不必再安装，可以直接跳过这一步。

##### ①下载mysql-jdbc-driver.jar

##### 下载地址:

```
 jdbc连接mysql驱动的文件目录，可去官网下载:https://dev.mysql.com/downloads/connector/j/
```

此处我使用的jar的版本为:mysql-connector-java-5.1.46.jar.后面我们会把这个jar包放在logstash的config目录下面


##### ②下载logstash-6.3.0

下载命令:

```shell
sudo wget  https://artifacts.elastic.co/downloads/logstash/logstash-6.3.0.zip
```

解压命令:

```shell
yum install -y unzip
unzip logstash-6.3.0.zip
```

##### ③elasticSearch与数据库表的对应关系

| ES   | MYSQL        |
| :--- | :----------- |
| 索引 | 数据库       |
| 类型 | 数据表       |
| 文档 | 数据表的一行 |
| 属性 | 数据表的一列 |

##### ④建立测试数据表

sql语句如下:

```sql
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `age` int(11) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES ('1', '调度', '12', '2019-06-13 19:24:55');
INSERT INTO `student` VALUES ('2', '李四', '13', '2019-06-13 19:24:55');
INSERT INTO `student` VALUES ('3', '王五', '15', '2019-06-13 19:24:55');
INSERT INTO `student` VALUES ('4', '赵六', '18', '2019-06-13 21:01:32');
INSERT INTO `student` VALUES ('5', '的地方', '52', '2019-06-13 21:13:51');
INSERT INTO `student` VALUES ('6', '测试', '45', '2019-06-13 21:17:31');
```

在logstatsh的目录下面建立my_logstash文件夹,里面建立myjdbc.conf:(这个仅供参考 实际不使用)

```shell
input {
  jdbc {
    # mysql相关jdbc配置
    jdbc_connection_string => "jdbc:mysql://IP:3306/test?useUnicode=true&characterEncoding=utf-8&useSSL=false"
    jdbc_user => "****"
    jdbc_password => "****"

    # jdbc连接mysql驱动的文件目录，可去官网下载:https://dev.mysql.com/downloads/connector/j/
    jdbc_driver_library => "./config/mysql-connector-java-5.1.46.jar"
    # the name of the driver class for mysql
    jdbc_driver_class => "com.mysql.jdbc.Driver"
    jdbc_paging_enabled => true
    jdbc_page_size => "50000"

    jdbc_default_timezone =>"Asia/Shanghai"

    # mysql文件, 也可以直接写SQL语句在此处，如下：
    #statement => "select * from student where update_time >= :sql_last_value"
     statement => "select * from student"
    # statement_filepath => "./config/jdbc.sql"

    # 这里类似crontab,可以定制定时操作，比如每分钟执行一次同步(分 时 天 月 年)
    schedule => "* * * * *"
    #type => "jdbc"

    # 是否记录上次执行结果, 如果为真,将会把上次执行到的 tracking_column 字段的值记录下来,保存到      last_run_metadata_path 指定的文件中
    #record_last_run => true

    # 是否需要记录某个column 的值,如果record_last_run为真,可以自定义我们需要 track 的 column 名称，此时该参数就要为 true. 否则默认 track 的是 timestamp 的值.
    use_column_value => true

    # 如果 use_column_value 为真,需配置此参数. track 的数据库 column 名,该 column 必须是递增的. 一般是mysql主键
    tracking_column => "update_time"

    tracking_column_type => "timestamp"

    last_run_metadata_path => "./logstash_capital_bill_last_id"

    # 是否清除 last_run_metadata_path 的记录,如果为真那么每次都相当于从头开始查询所有的数据库记录
    clean_run => false

    #是否将 字段(column) 名称转小写
    lowercase_column_names => false
  }
}

output {
  elasticsearch {
    hosts => "192.168.142.128:9200"
    index => "resource"
    user => "elastic"
    password => "elastic"
    document_id => "%{id}"
    template_overwrite => true
  }

  # 这里输出调试，正式运行时可以注释掉
  stdout {
      codec => json_lines
  } 
}
```

在logstash的config目录下面新建logstash-mysql.conf:

```shell
input {
  jdbc {
    jdbc_driver_library => "/usr/local/logstash-6.3.0/config/mysql-connector-java-5.1.46.jar"
    jdbc_driver_class => "com.mysql.jdbc.Driver"
    jdbc_connection_string => "jdbc:mysql://192.168.142.128:3306/test?characterEncoding=UTF-8&useSSL=false"
    jdbc_user => "root"
    jdbc_password => "root"
    statement => "SELECT * FROM student WHERE update_time > :sql_last_value"
    jdbc_paging_enabled => "true"
    jdbc_page_size => "50000"
    schedule => "* * * * *"
  }
}

filter {
   json {
        source => "message"
        remove_field => ["message"]
    }
}

output {
  stdout {
    codec => rubydebug
  }
  elasticsearch {
    hosts => "192.168.142.128:9200"   
    index => "student"
    user => "elastic"
    password => "elastic"
  }        
}
```

启动命令,开始导入数据:

```shell
/data/logstash-6.3.0/bin/logstash -f /data/logstash-6.3.0/config/logstash-mysql.conf
```

后台运行:

```java
nohup /data/logstash-6.3.0/bin/logstash -f /data/logstash-6.3.0/config/logstash-mysql.conf &
```

#### 8.安装IK分词

##### ①git地址,直接运行如下命令:

```java
bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v6.5.1/elasticsearch-analysis-ik-6.5.1.zip
```

##### ②在elasticsearch-6.5.0主目录下的plugins目录新建一个ik文件夹

##### ③解压上面的zip包到ik目录,重启es即可

#### 9.设置JDBC连接密码

##### ①设置elasticsearch配置文件,然后重启elasticSearch

```java
vim /data/elasticsearch-6.5.1/config/elasticsearch.yml
    
-- 添加如下内容:

http.cors.enabled: true
http.cors.allow-origin: "*"
http.cors.allow-headers: Authorization
xpack.security.enabled: true
xpack.security.transport.ssl.enabled: true

```

##### ②设置密码

```shell
cd /data/elasticsearch-6.5.1/bin
./elasticsearch-setup-passwords interactive
```

```java
Please confirm that you would like to continue [y/N]y


Enter password for [elastic]: elastic
Reenter password for [elastic]: elastic
Passwords do not match.
Try again.
Enter password for [elastic]:
Reenter password for [elastic]:
Enter password for [apm_system]:
Reenter password for [apm_system]:
Enter password for [kibana]:
Reenter password for [kibana]:
Enter password for [logstash_system]:
Reenter password for [logstash_system]:
Enter password for [beats_system]:
Reenter password for [beats_system]:
Enter password for [remote_monitoring_user]:
Reenter password for [remote_monitoring_user]:
Changed password for user [apm_system]
Changed password for user [kibana]
Changed password for user [logstash_system]
Changed password for user [beats_system]
Changed password for user [remote_monitoring_user]
Changed password for user [elastic]

```

##### ③修改kibana

```java
vim /data/kibana-6.2.3-linux-x86_64/config/kibana.yml
    
-- 添加如下内容:

elasticsearch.username: "elastic"
elasticsearch.password: "elastic"
```

修改密码:

```java
POST /_security/user/elastic/_password
{
  "password": "123456"
}

修改密码之后，需要重新设置kibana的配置文件，才可以重新使用kibana
```

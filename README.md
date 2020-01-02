# elasticSearch-sql

#### 1.安装ElasticSearch6.5.1

​		解压相关的ElasticSearch6.5.1的tar包到目录下,如果我们需要使用JDBC来连接的话是需要到白金版以上的,以下为将基础版破解为白金版的方法.

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

##### 4.使用SpringBoot连接ElasticSearch-sql+mybatis的操作:

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
        <dss.common.version>3.0.0-RC04</dss.common.version>
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
https://github.com/fengcharly/elasticSearch-sql
```


# mybatis-xmlreload-spring-boot-starter
mybatis-xmlreload-spring-boot-starter 支持xml文件热更新功能。

**Spring Boot3.0请使用3.0.1+版本, Spring Boot2.0请使用2.0.1+版本**

# 项目特点
- 

# 安装
- maven
```xml
<dependency>
    <groupId>com.wayn</groupId>
    <artifactId>mybatis-xmlreload-spring-boot-starter</artifactId>
    <version>3.0.1</version>
</dependency>
```

# 使用
通过 maven 公共仓库或者下载本项目源码，在项目pom文件中写入本项目坐标，默认自动启用 xml 热更新功能，想要关闭的话通过设置 `mybatis.xml-reload.enabled`
为 false。配置列表
- `mybatis.xml-reload.enabled`：是否开启 xml 热更新，true开启，false不开启，默认为true
- `mybatis.xml-reload.mapper-locations`：xml文件位置，eg: `classpath*:mapper/**/*Mapper.xml`


# mybatis-xmlreload-spring-boot-starter
mybatis-xmlreload-spring-boot-starter 支持xml文件热更新功能。

# 1. 安装
- maven
```xml
<dependency>
    <groupId>com.wayn</groupId>
    <artifactId>mybatis-xmlreload-spring-boot-starter</artifactId>
    <version>1.1.0</version>
</dependency>
```

# 2. 使用方式
引入 maven 依赖后默认自动启用 xml 热更新功能，想要关闭的话通过设置 `mybatis.xml-reload.enabled`
为 false。配置列表
- `mybatis.xml-reload.enabled`：是否开启 xml 热更新，true开启，false不开启，默认为true
- `mybatis.xml-reload.mapper-locations`：xml文件位置，eg: `classpath*:mapper/**/*Mapper.xml`

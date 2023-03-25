# mybatis-xmlreload-spring-boot-starter
mybatis-xmlreload-spring-boot-starter 支持xml文件热更新功能。

| 分支名称                                                                                             | Spring Boot版本 |
|--------------------------------------------------------------------------------------------------|---------------|
| [main](https://github.com/wayn111/mybatis-xmlreload-spring-boot-starter)                         | 3.0.4         |
| [springboot2](https://github.com/wayn111/mybatis-xmlreload-spring-boot-starter/tree/springboot2) | 2.2.7.RELEASE 

# 项目特点
- 修改项目加载的xml文件的为自己项目resources目录下的xml文件，而不是项目编译后的target目录下的xml文件，这样做的好处是自己resources目录的xml文件修改后可以立即生效，而不用等待项目编译
- xml文件监听，修改xml文件会立即触发xml文件热加载，并且只重新加载修改过xml文件，而不是所有的xml文件。

# 安装
- maven
```xml
<dependency>
    <groupId>io.github.wayn111</groupId>
    <artifactId>mybatis-xmlreload-spring-boot-starter</artifactId>
    <version>3.0.3.m1</version>
</dependency>
```

# 使用
通过 maven 公共仓库或者下载本项目源码，在项目pom文件中写入本项目坐标，默认自动启用 xml 热更新功能，想要关闭的话通过设置 `mybatis.xml-reload.enabled`
为 false。配置列表
```yml
mybatis-xml-reload:
  # 是否开启 xml 热更新，true开启，false不开启，默认为false
  enabled: true 
  # xml文件位置，eg: `classpath*:mapper/**/*Mapper.xml`
  mapper-locations: classpath:mapper/*Mapper.xml
```

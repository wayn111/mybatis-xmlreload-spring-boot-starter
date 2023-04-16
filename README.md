# mybatis-xmlreload-spring-boot-starter
mybatis-xmlreload-spring-boot-starter 支持xml文件热更新功能。

| 分支名称                                                                                             | Spring Boot版本 |
|--------------------------------------------------------------------------------------------------|---------------|
| [main](https://github.com/wayn111/mybatis-xmlreload-spring-boot-starter)                         | 3.0.4         |
| [springboot2](https://github.com/wayn111/mybatis-xmlreload-spring-boot-starter/tree/springboot2) | 2.2.7.RELEASE 
---
# 原理
- 修改 xml 文件的加载逻辑。在普通的 mybatis-spring 项目中，默认只会加载项目编译过后的 xml 文件，也就是 target 目录下的 xml 文件。但是在mybatis-xmlreload-spring-boot-starter中，修改了这一点，它会加载项目 resources 目录下的 xml 文件，这样用户对于 resources 目录下 xml 文件的修改操作是可以立即触发热加载的。
- 通过 io.methvin.directory-watcher 项目来监听 xml 文件的修改操作，它底层是通过 java.nio 的WatchService 来实现，当我们监听了整个 resources 目录后，xml 文件的修改会立马触发 MODIFY 事件。
- 通过 mybatis-spring 项目原生的 xmlMapperBuilder.parse() 方法重新加载解析修改过后的 xml 文件来保证项目对于 Mybatis 的兼容性处理。


# 安装方式
在 Spring Boot3.0 中，mybatis-xmlreload-spring-boot-starter在 Maven 项目提供坐标地址如下：

```xml
<dependency>
    <groupId>com.wayn</groupId>
    <artifactId>mybatis-xmlreload-spring-boot-starter</artifactId>
    <version>3.0.3.m1</version>
</dependency>
```
在 Spring Boot2.0 Maven 项目提供坐标地址如下：
```xml
<dependency>
    <groupId>com.wayn</groupId>
    <artifactId>mybatis-xmlreload-spring-boot-starter</artifactId>
    <version>2.0.1.m1</version>
</dependency>
```

# 使用配置
**mybatis-xmlreload-spring-boot-starter** 目前只有两个配置属性。
- `mybatis-xml-reload.enabled` 默认是 false， 也就是不启用 xml 文件的热加载功能，想要开启的话通过在项目配置文件中设置 `mybatis-xml-reload.enabled` 为true。
- `mybatis-xml-reload.mapper-locations`需要热加载的 xml 文件路径，这个属性需要手动填写，跟项目中的 `mybatis.mapper-locations` 保持一致即可。如果是多数据源配置，这里可以用逗号分割填写多个路径
```yml
# mybatis xml文件热加载配置
mybatis-xml-reload:
  # 是否开启 xml 热更新，true开启，false不开启，默认为false
  enabled: true
  # xml文件路径，可以填写多个，逗号分隔。
  # eg: `classpath*:mapper/**/*Mapper.xml,classpath*:other/**/*Mapper.xml`
  mapper-locations: classpath:mapper/*Mapper.xml
```
# 学习交流
> 如果有任何问题，欢迎提交Issue或加我微信告知，方便互相交流反馈～ 💘。最后，喜欢的话麻烦给我个star

关注公众号：waynblog，每周更新最新技术文章。回复关键字：
- **学习**：加群交流，群内问题都会一一解答。

<img src="images/wx-mp-code.png" width = "100" />

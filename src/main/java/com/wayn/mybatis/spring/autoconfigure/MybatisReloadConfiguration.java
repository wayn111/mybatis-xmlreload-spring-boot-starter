package com.wayn.mybatis.spring.autoconfigure;

import io.methvin.watcher.DirectoryWatcher;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

/**
 * mybatis xml file hot reload configuration.
 * if <b>mybatis.xml-reload</b> is true, will enable this configuration.
 *
 * @author wayn
 */
@Component
@ConditionalOnProperty(value = "mybatis.xml-reload", matchIfMissing = true)
public class MybatisReloadConfiguration extends ApplicationObjectSupport implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(MybatisReloadConfiguration.class);
    public static final PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();


    private final MybatisProperties properties;

    public MybatisReloadConfiguration(MybatisProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws IOException {
        Map<String, SqlSessionFactory> beansOfType = getApplicationContext().getBeansOfType(SqlSessionFactory.class);
        String[] locationPatterns = properties.getMapperLocations();
        List<Resource> mapperLocations = Arrays.stream(properties.resolveMapperLocations()).collect(Collectors.toList());

        List<Path> rootPaths = new ArrayList<>();
        for (String locationPattern : locationPatterns) {
            String[] split = locationPattern.split("/");
            String rootDir = split[0];
            Path rootPath = Path.of(pathMatchingResourcePatternResolver.getResources(rootDir)[0].getFile().getAbsolutePath());
            if (!rootDir.contains("classpath:")) {
                String compilerDir = rootDir.split(":", 2)[1];
                File mapperDir = new File("src/main/resources/" + compilerDir);
                if (mapperDir.exists()) {
                    rootPath = Path.of(mapperDir.getAbsolutePath());
                    File[] files = mapperDir.listFiles();
                    mapperLocations.addAll(Arrays.stream(files != null ? files : new File[0]).map(FileSystemResource::new).toList());
                }
            }
            rootPaths.add(rootPath);
        }


        DirectoryWatcher watcher = DirectoryWatcher.builder()
                .paths(rootPaths) // or use paths(directoriesToWatch)
                .listener(event -> {
                    switch (event.eventType()) {
                        case CREATE: /* file created */
                            break;
                        case MODIFY: /* file modified */
                            Path modifyPath = event.path();
                            String absolutePath = modifyPath.toFile().getAbsolutePath();
                            LOGGER.info("mybatis xml file has changed:" + modifyPath);
                            for (SqlSessionFactory sqlSessionFactory : beansOfType.values()) {
                                try {
                                    Configuration targetConfiguration = sqlSessionFactory.getConfiguration();
                                    Class<? extends Configuration> aClass = targetConfiguration.getClass();
                                    Set<String> loadedResources = (Set<String>) getFieldValue(targetConfiguration, aClass, "loadedResources");
                                    loadedResources.clear();

                                    Map<String, ResultMap> resultMaps = (Map<String, ResultMap>) getFieldValue(targetConfiguration, aClass, "resultMaps");
                                    Map<String, XNode> sqlFragmentsMaps = (Map<String, XNode>) getFieldValue(targetConfiguration, aClass, "sqlFragments");
                                    Map<String, MappedStatement> mappedStatementMaps = (Map<String, MappedStatement>) getFieldValue(targetConfiguration, aClass, "mappedStatements");

                                    for (Resource mapperLocation : mapperLocations) {
                                        if (!absolutePath.equals(mapperLocation.getFile().getAbsolutePath())) {
                                            continue;
                                        }
                                        XPathParser parser = new XPathParser(mapperLocation.getInputStream(), true, targetConfiguration.getVariables(), new XMLMapperEntityResolver());
                                        XNode mapperXnode = parser.evalNode("/mapper");
                                        List<XNode> resultMapNodes = mapperXnode.evalNodes("/mapper/resultMap");
                                        String namespace = mapperXnode.getStringAttribute("namespace");
                                        for (XNode xNode : resultMapNodes) {
                                            String id = xNode.getStringAttribute("id", xNode.getValueBasedIdentifier());
                                            resultMaps.remove(namespace + "." + id);
                                        }

                                        List<XNode> sqlNodes = mapperXnode.evalNodes("/mapper/sql");
                                        for (XNode sqlNode : sqlNodes) {
                                            String id = sqlNode.getStringAttribute("id", sqlNode.getValueBasedIdentifier());
                                            sqlFragmentsMaps.remove(namespace + "." + id);
                                        }

                                        List<XNode> msNodes = mapperXnode.evalNodes("select|insert|update|delete");
                                        for (XNode msNode : msNodes) {
                                            String id = msNode.getStringAttribute("id", msNode.getValueBasedIdentifier());
                                            mappedStatementMaps.remove(namespace + "." + id);
                                        }
                                        try {
                                            XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(mapperLocation.getInputStream(),
                                                    targetConfiguration, mapperLocation.toString(), targetConfiguration.getSqlFragments());
                                            xmlMapperBuilder.parse();
                                        } catch (Exception e) {
                                            LOGGER.error(e.getMessage(), e);
                                        }
                                        LOGGER.info("Parsed mapper file: '" + mapperLocation + "'");
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            break;
                        case DELETE: /* file deleted */
                            break;
                    }
                })
                // .fileHashing(false) // defaults to true
                // .logger(logger) // defaults to LoggerFactory.getLogger(DirectoryWatcher.class)
                // .watchService(watchService) // defaults based on OS to either JVM WatchService or the JNA macOS WatchService
                .build();
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setName("xml-reload");
            thread.setDaemon(true);
            return thread;
        };
        watcher.watchAsync(new ScheduledThreadPoolExecutor(1, threadFactory));
    }

    /**
     * Use reflection to get the field value.
     */
    private static Object getFieldValue(Configuration targetConfiguration, Class<? extends Configuration> aClass,
                                        String filed) throws NoSuchFieldException, IllegalAccessException {
        Field resultMapsField = aClass.getDeclaredField(filed);
        resultMapsField.setAccessible(true);
        return resultMapsField.get(targetConfiguration);
    }
}
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * mybatis xml file hot reload configuration.
 * if <b>mybatis.xml-reload</b> is true, will enable this configuration.
 *
 * @author wayn
 */
@Component
@ConditionalOnProperty(value = "mybatis.xml-reload.enabled", matchIfMissing = true)
public class MybatisReloadConfiguration extends ApplicationObjectSupport implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(MybatisReloadConfiguration.class);
    public static final PathMatchingResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();

    public static final String CLASS_PATH_TARGET = File.separator + "target" + File.separator + "classes";
    public static final String MAVEN_RESOURCES = "/src/main/resources";
    public static final Pattern CLASS_PATH_PATTERN = Pattern.compile("(classpath\\*?:)(\\w*)");

    @Value("${mybatis.xml-reload.mapper-locations:}")
    private String[] xmlReloadMapperLocations;

    @Override
    public void afterPropertiesSet() throws IOException {
        Map<String, SqlSessionFactory> beansOfType = getApplicationContext().getBeansOfType(SqlSessionFactory.class);
        List<Resource> mapperLocationsTmp = Stream.of(Optional.of(xmlReloadMapperLocations).orElse(new String[0]))
                .flatMap(location -> Stream.of(getResources(location))).collect(Collectors.toList());

        List<Resource> mapperLocations = new ArrayList<>(mapperLocationsTmp.size() * 2);
        Set<Path> locationPatternSet = new HashSet<>();
        for (Resource mapperLocation : mapperLocationsTmp) {
            mapperLocations.add(mapperLocation);
            String absolutePath = mapperLocation.getFile().getAbsolutePath();
            File tmpFile = new File(absolutePath.replace(CLASS_PATH_TARGET, MAVEN_RESOURCES));
            if (tmpFile.exists()) {
                locationPatternSet.add(Path.of(tmpFile.getParent()));
                FileSystemResource fileSystemResource = new FileSystemResource(tmpFile);
                mapperLocations.add(fileSystemResource);
            }
        }

        List<Path> rootPaths = new ArrayList<>();
        rootPaths.addAll(locationPatternSet);
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
                                    Class<?> tClass = targetConfiguration.getClass(), aClass = targetConfiguration.getClass();
                                    if (targetConfiguration.getClass().getSimpleName().equals("MybatisConfiguration")) {
                                        aClass = Configuration.class;
                                    }
                                    Set<String> loadedResources = (Set<String>) getFieldValue(targetConfiguration, aClass, "loadedResources");
                                    loadedResources.clear();

                                    Map<String, ResultMap> resultMaps = (Map<String, ResultMap>) getFieldValue(targetConfiguration, tClass, "resultMaps");
                                    Map<String, XNode> sqlFragmentsMaps = (Map<String, XNode>) getFieldValue(targetConfiguration, tClass, "sqlFragments");
                                    Map<String, MappedStatement> mappedStatementMaps = (Map<String, MappedStatement>) getFieldValue(targetConfiguration, tClass, "mappedStatements");

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
                                    LOGGER.error(e.getMessage(), e);
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

    private Resource[] getResources(String location) {
        try {
            return patternResolver.getResources(location);
        } catch (IOException e) {
            return new Resource[0];
        }
    }

    /**
     * Use reflection to get the field value.
     */
    private static Object getFieldValue(Configuration targetConfiguration, Class<?> aClass,
                                        String filed) throws NoSuchFieldException, IllegalAccessException {
        Field resultMapsField = aClass.getDeclaredField(filed);
        resultMapsField.setAccessible(true);
        return resultMapsField.get(targetConfiguration);
    }
}

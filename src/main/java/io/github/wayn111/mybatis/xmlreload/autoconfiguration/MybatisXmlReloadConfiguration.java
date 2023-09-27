package io.github.wayn111.mybatis.xmlreload.autoconfiguration;

import io.github.wayn111.mybatis.xmlreload.MybatisXmlReload;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;

/**
 * mybatis xml热加载自动配置
 *
 * @author wayn
 */
@Configuration
@EnableConfigurationProperties({MybatisXmlReloadProperties.class})
public class MybatisXmlReloadConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MybatisXmlReload mybatisXmlReload(MybatisXmlReloadProperties prop,
                                             List<SqlSessionFactory> sqlSessionFactories) throws IOException {
        MybatisXmlReload mybatisXmlReload = new MybatisXmlReload(prop, sqlSessionFactories);
        if (prop.getEnabled()) {
            mybatisXmlReload.xmlReload();
        }
        return mybatisXmlReload;
    }

    @Bean
    public static LazyInitializationExcludeFilter integrationLazyInitializationExcludeFilter() {
        return LazyInitializationExcludeFilter.forBeanTypes(MybatisXmlReload.class);
    }
}

package io.github.wayn111.mybatis.xmlreload.autoconfiguration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * mybatis-xml-reload配置类
 */
@ConfigurationProperties(prefix = "mybatis-xml-reload")
public class MybatisXmlReloadProperties {
    private Boolean enabled;

    private String[] mapperLocations;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String[] getMapperLocations() {
        return mapperLocations;
    }

    public void setMapperLocations(String[] mapperLocations) {
        this.mapperLocations = mapperLocations;
    }
}

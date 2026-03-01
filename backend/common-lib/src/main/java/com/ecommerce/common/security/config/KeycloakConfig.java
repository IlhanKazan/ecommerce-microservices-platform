package com.ecommerce.common.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt.auth.converter")
public class KeycloakConfig {

    private String resourceId;
    private String principalAttribute;

    public String getPrincipalAttribute() {
        return principalAttribute;
    }

    public void setPrincipalAttribute(String principalAttribute){
        this.principalAttribute = principalAttribute;
    }

    public String getResourceId(){
        return resourceId;
    }

    public void setResourceId(String resourceId){
        this.resourceId = resourceId;
    }

}


package com.gantoniadis.cargopulse.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    
    private String mobileSecretHeaderKey;
    private String mobileSecretHeaderValue;
    private Cookie cookie = new Cookie();
    
    @Data
    public static class Cookie {
        private boolean sslTransfer;
    }
}
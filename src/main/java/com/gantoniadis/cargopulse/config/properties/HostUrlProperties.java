package com.gantoniadis.cargopulse.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "host.url")
public class HostUrlProperties {
    
    private String frontend;
    private String mobileApp;
}
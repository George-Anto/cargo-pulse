package com.gantoniadis.cargopulse.config.properties;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "monitoring")
@RequiredArgsConstructor
public class MonitoringProperties {
    
    private final Prometheus prometheus = new Prometheus();
    private final Grafana grafana = new Grafana();
    
    @Data
    public static class Prometheus {
        private String ipExpression;
        private int port;
    }
    
    @Data
    public static class Grafana {
        private int port;
    }
}
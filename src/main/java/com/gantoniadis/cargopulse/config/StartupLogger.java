package com.gantoniadis.cargopulse.config;

import com.gantoniadis.cargopulse.service.ApplicationUrlProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupLogger implements ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationUrlProvider urlProvider;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${swagger.path}")
    private String swaggerPath;

    @Value("${monitoring.prometheus.port}")
    private int prometheusPort;

    @Value("${monitoring.grafana.port}")
    private int grafanaPort;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        String baseUrl = urlProvider.getApplicationBaseUrl();
        String host = urlProvider.getDynamicHostAddress();

        log.info("- - - - - - - - - - - - - - - - - - - - - - - - ");
        log.info("Application '{}' started at {}/", appName, baseUrl);
        log.info("Swagger UI available at {}{}", baseUrl, swaggerPath);

        // Log Monitoring Tools
        log.info("- - - Start Docker Containers to be able to access the monitoring tools - - -");
        log.info("Prometheus Dashboard: http://{}:{}", host, prometheusPort);
        log.info("Grafana Dashboard: http://{}:{}", host, grafanaPort);

        log.info("Active profile(s): {}", String.join(", ", event.getApplicationContext().getEnvironment().getActiveProfiles()));
        log.info("- - - - - - - - - - - - - - - - - - - - - - - - ");
    }
}
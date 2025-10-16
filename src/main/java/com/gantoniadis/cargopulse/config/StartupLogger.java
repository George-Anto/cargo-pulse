package com.gantoniadis.cargopulse.config;

import com.gantoniadis.cargopulse.service.ApplicationUrlProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import com.gantoniadis.cargopulse.config.properties.ApplicationProperties;
import com.gantoniadis.cargopulse.config.properties.SwaggerProperties;
import com.gantoniadis.cargopulse.config.properties.MonitoringProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupLogger implements ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationUrlProvider urlProvider;
    private final ApplicationProperties applicationProperties;
    private final SwaggerProperties swaggerProperties;
    private final MonitoringProperties monitoringProperties;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        String baseUrl = urlProvider.getApplicationBaseUrl();
        String host = urlProvider.getDynamicHostAddress();

        log.info("- - - - - - - - - - - - - - - - - - - - - - - - ");
        log.info("Application '{}' started at {}/", applicationProperties.getName(), baseUrl);
        log.info("Swagger UI available at {}{}", baseUrl, swaggerProperties.getPath());

        // Log Monitoring Tools
        log.info("- - - Start Docker Containers to be able to access the monitoring tools - - -");
        log.info("Prometheus Dashboard: http://{}:{}", host, monitoringProperties.getPrometheus().getPort());
        log.info("Grafana Dashboard: http://{}:{}", host, monitoringProperties.getGrafana().getPort());

        log.info("Active profile(s): {}", String.join(", ", event.getApplicationContext().getEnvironment().getActiveProfiles()));
        log.info("- - - - - - - - - - - - - - - - - - - - - - - - ");
    }
}

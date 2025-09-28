package com.gantoniadis.cargopulse.config;

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

    @Value("${spring.application.name}")
    private String appName;

    @Value("${swagger.path}")
    private String swaggerPath;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String host = getDynamicHost();
        int port = event.getApplicationContext().getEnvironment().getProperty("server.port", Integer.class, 8080);

        log.info("- - - - - - - - - - - - - - - - - - - - - - - - ");
        log.info("Application '{}' started at http://{}:{}/", appName, host, port);
        log.info("Swagger UI available at http://{}:{}{}", host, port, swaggerPath);
        log.info("Active profile(s): {}", String.join(", ", event.getApplicationContext().getEnvironment().getActiveProfiles()));
        log.info("- - - - - - - - - - - - - - - - - - - - - - - - ");
    }

    private String getDynamicHost() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("Failed to determine host address, defaulting to 'localhost'", e);
            return "localhost";
        }
    }
}

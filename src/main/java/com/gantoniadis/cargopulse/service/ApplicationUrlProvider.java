package com.gantoniadis.cargopulse.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Component
@Getter
@RequiredArgsConstructor
public class ApplicationUrlProvider implements EnvironmentAware {

    private Environment environment;

    private String applicationBaseUrl;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        String protocol = environment.getProperty("server.ssl.key-store") != null ? "https" : "http";
        int port = environment.getProperty("server.port", Integer.class, 8080);
        String host = getDynamicHostAddress();

        this.applicationBaseUrl = String.format("%s://%s:%d", protocol, host, port);
    }

    public String getDynamicHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Failed to determine host address, defaulting to 'localhost'", e);
            return "localhost";
        }
    }
}
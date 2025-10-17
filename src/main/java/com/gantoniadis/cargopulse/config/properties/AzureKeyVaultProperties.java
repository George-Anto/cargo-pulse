package com.gantoniadis.cargopulse.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@Component
@ConfigurationProperties(prefix = "azure.keyvault")
@Validated
public class AzureKeyVaultProperties {

    @NotBlank(message = "Azure Key Vault URI cannot be blank")
    @Pattern(regexp = "^https://.*\\.vault\\.azure\\.net/?$", message = "Invalid Azure Key Vault URI format")
    private String uri;
}

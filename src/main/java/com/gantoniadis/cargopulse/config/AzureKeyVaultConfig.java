package com.gantoniadis.cargopulse.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.gantoniadis.cargopulse.config.properties.AzureKeyVaultProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simple configuration class to load secrets from Azure Key Vault.
 * Configuration properties are defined in AzureKeyVaultProperties class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AzureKeyVaultConfig {

    private final AzureKeyVaultProperties azureKeyVaultProperties;

    @PostConstruct
    public void initializeAndLoadSecrets() {

        try {
            log.debug("Initializing properties from Azure Key Vault at: {}",
                    azureKeyVaultProperties.getUri());

            // Create a SecretClient with DefaultAzureCredential
            /*
             * DefaultAzureCredentialBuilder automatically tries different authentication methods:
             * 1. Environment Variables (AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET)
             * 2. Managed Identity
             * 3. Visual Studio Code credentials
             * 4. Azure CLI credentials
             * 5. IntelliJ credentials
             */
            SecretClient secretClient = new SecretClientBuilder()
                    .vaultUrl(azureKeyVaultProperties.getUri())
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();

            // Retrieve secrets and set as system properties for primary database
            String dbUrl = secretClient.getSecret("cargo-pulse-db-url").getValue();
            String dbUsername = secretClient.getSecret("cargo-pulse-db-username").getValue();
            String dbPassword = secretClient.getSecret("cargo-pulse-db-password").getValue();

            System.setProperty("CARGOPULSE_DB_URL", dbUrl);
            System.setProperty("CARGOPULSE_DB_USER", dbUsername);
            System.setProperty("CARGOPULSE_DB_PASSWORD", dbPassword);

            log.debug("Azure Key Vault secrets loaded successfully");
        } catch (Exception e) {
            log.error("Error loading Azure Key Vault secrets: {}", e.getMessage());
        }
    }
}

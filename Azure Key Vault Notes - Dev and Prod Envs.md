# Azure Key Vault Dev and (Future) Prod Setup Process Notes

This guide documents the final, optimized configuration using the **`spring-cloud-azure-starter-keyvault`** for securely loading application secrets via the Spring Environment. This approach leverages Azure Identity and eliminates the need for manual order enforcement or custom secret-loading Java code.

***

## 1. Azure Setup (Prerequisites)

1.  **Key Vault URI/Endpoint:** You must have a Key Vault provisioned. Its URI/Endpoint is supplied to the application via an environment variable.
    * **Environment Variable:** `AZURE_KEYVAULT_ENDPOINT`
2.  **Secret Naming:** Secrets in the Key Vault **MUST** be named to match the property placeholders in your `application.yml` (e.g., the secret named `cargo-pulse-db-url` is used as `${cargo-pulse-db-url}`).
3.  **Access Control:** The application's identity (Service Principal for Dev/Local, Managed Identity for Prod/AKS) must be granted the **Key Vault Secrets User** role (or equivalent **Get/List** policy permissions) on the Key Vault resource.

***

## 2. Spring Boot Integration: Authentication Configuration

The Spring Cloud Azure Starter uses the `DefaultAzureCredential` to handle authentication automatically, based on the environment variables provided.

### Dev/Local Environment (Service Principal Authentication)

In the local development environment, authentication is driven by an Azure AD Service Principal, whose credentials are provided as environment variables.

| Variable Name | Requirement | Configuration | Purpose |
| :--- | :--- | :--- | :--- |
| **`AZURE_KEYVAULT_ENDPOINT`** | **Required** | Environment Variable | Specifies the Key Vault URL. |
| **`AZURE_CLIENT_ID`** | **Required** | Environment Variable | Service Principal (App Registration) ID for authentication. |
| **`AZURE_TENANT_ID`** | **Required** | Environment Variable | Azure Directory/Tenant ID. |
| **`AZURE_CLIENT_SECRET`** | **Required** | Environment Variable | Service Principal secret. |

***

### Production Environment (Managed Identity/Workload Identity Authentication)

In the production environment (e.g., AKS/Workload Identity), the application authenticates using a Managed Identity, which requires no secret environment variables.

| Variable Name | Requirement | Configuration | Purpose |
| :--- | :--- | :--- | :--- |
| **`AZURE_KEYVAULT_ENDPOINT`** | **Required** | Application Configuration | Specifies the Key Vault URL. |
| **`AZURE_CLIENT_ID`** | **DO NOT PASS** | N/A | Authentication is handled by the **Managed Identity** assigned to the pod. |
| **`AZURE_TENANT_ID`** | **DO NOT PASS** | N/A | |
| **`AZURE_CLIENT_SECRET`** | **DO NOT PASS** | N/A | |

* **Authentication Flow:** The `DefaultAzureCredential` automatically detects and uses the **Managed Identity** assigned to the host environment (like an AKS pod with Workload Identity) to authenticate to Key Vault.

***

## 3. Configuration Cleanup

The following components were successfully **removed** as they are no longer needed with the native Azure Property Source approach:

* Custom Java classes for secret loading (e.g., `AzureKeyVaultConfig.java`).
* The `System.setProperty()` calls inside `@PostConstruct` methods.
* All manual dependency enforcement annotations (`@DependsOn` on `DataSource` or `RedisConfig`).
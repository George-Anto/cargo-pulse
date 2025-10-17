# Azure Key Vault Dev and (Future) Prod Setup Process Notes

This guide documents the steps taken to configure the Azure Key Vault and the Spring Boot application for securely loading database credentials in the local development profile.

## 1. Azure Setup (Prerequisites)

1.  **Key Vault Creation:**
    * Provisioned an Azure Key Vault instance (e.g., `cargo-pulse-dev-kv`) to securely store application secrets.
2.  **Service Principal Creation (App Registration):**
    * Created an **Azure AD Application Registration** to serve as the Spring Boot app's identity (Service Principal).
    * This process yielded the three required credentials:
        * **Application (Client) ID** (`AZURE_CLIENT_ID`)
        * **Directory (Tenant) ID** (`AZURE_TENANT_ID`)
        * **Client Secret Value** (`AZURE_CLIENT_SECRET`)
3.  **Access Control:**
    * The Service Principal was granted **Key Vault Secrets User** role (or equivalent **Get/List** access policy permissions) on the Key Vault resource to allow it to read the stored secrets.

***

## 2. Spring Boot Integration (Local Authentication)

The application uses the **Azure Identity SDK**'s **`DefaultAzureCredentialBuilder`** for authentication.

1.  **Credential Passing:** The Service Principal credentials are provided to the Spring Boot application via **Environment Variables** in the IntelliJ Run Configuration (or local shell).
    * **Variables Used:** `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, and `AZURE_CLIENT_SECRET`.
2.  **Secret Retrieval:** The custom `AzureKeyVaultConfig` class performs the following:
    * It uses the `SecretClient` to connect to the Key Vault.
    * It retrieves the database secrets (e.g., `cargo-pulse-db-url`).
    * It immediately sets these retrieved values as **Java System Properties** (e.g., `CARGOPULSE_DB_URL`, `CARGOPULSE_DB_USER`, `CARGOPULSE_DB_PASSWORD`) using `System.setProperty()`.

***

## 3. Order Enforcement (Solving the Timing Issue)

To prevent Spring Boot's automatic database connection from failing before the secrets are loaded:

1.  **Database Configuration Hook:** A configuration class (`DatabaseConfig.java` or similar) was created to manually define the `DataSource` bean (or the process that initializes it).
2.  **Dependency:** The `@DependsOn("azureKeyVaultConfig")` annotation was applied to the `DataSource` bean definition. This **forces** Spring to fully initialize the `AzureKeyVaultConfig` (running the `@PostConstruct` method and setting the system properties) *before* attempting to build the database connection pool.
***

## 4. Production Environment (AKS & Managed Identity)

The current application code is designed for seamless transition to production using **Managed Identity** in Azure Kubernetes Service (AKS).

1.  **No Code Change Required:** The use of **`DefaultAzureCredentialBuilder`** means the code automatically detects and prioritizes Managed Identity when deployed inside Azure.
2.  **Authentication Flow in AKS:**
    * When running in an AKS pod (configured with **Workload Identity**), the application **does not need** the `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, or `AZURE_CLIENT_SECRET` environment variables.
    * The `DefaultAzureCredentialBuilder` senses the underlying Azure resource identity and uses it to authenticate to Key Vault.
3.  **Deployment Steps:**
    * Create a **User-Assigned Managed Identity**.
    * Grant this **Managed Identity** the **Key Vault Secrets User** role (or equivalent) on the Key Vault.
    * Configure the AKS deployment (via **Workload Identity**) to assign this Managed Identity to the application pod.
    * **Crucially, ensure the Service Principal environment variables are NOT passed to the application container in AKS.**
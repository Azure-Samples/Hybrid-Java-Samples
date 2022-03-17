/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.azure.resourcemanager.keyvault.samples;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.util.serializer.JacksonAdapter;
import com.azure.core.util.serializer.SerializerEncoding;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.keyvault.models.SkuName;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.samples.Utils;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.SecretServiceVersion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Azure Stack Key Vault sample for managing secrets -
 * - Create a key vault
 * - Set a secret
 * - Get a secret
 * - Delete a key vault.
 */

public final class ManageKeyvaultSecret {
    /**
     * Main function which runs the actual sample.
     *
     * @param azureResourceManager instance of the azure client
     * @param location the Azure location
     * @param tokenCredential credential for secret client
     * @param objectId object ID of secret client credential
     * @return true if sample runs successfully
     */
    public static boolean runSample(AzureResourceManager azureResourceManager, String location,
                                    TokenCredential tokenCredential, String objectId) {
        final String vaultName = Utils.randomResourceName(azureResourceManager, "kv", 8);
        final String secretName = Utils.randomResourceName(azureResourceManager, "s", 8);
        final String secretValue = Utils.password();
        final String rgName = Utils.randomResourceName(azureResourceManager, "rgkvs", 16);
        try {


            //=============================================================
            // Create a key vault.

            System.out.println("Creating a key vault with name: " + vaultName);

            Vault vault = azureResourceManager.vaults().define(vaultName)
                    .withRegion(location)
                    .withNewResourceGroup(rgName)
                    .defineAccessPolicy()
                            .forObjectId(objectId)
                            .allowSecretAllPermissions()
                            .attach()
                    .withDeploymentEnabled()
                    .withTemplateDeploymentEnabled()
                    .withSku(SkuName.STANDARD)
                    .create();

            System.out.println("Created a key vault with name: " + vaultName);
            Utils.print(vault);


            //=============================================================
            // Set a secret.

            SecretClient secretClient = new SecretClientBuilder()
                    .vaultUrl(vault.vaultUri())
                    .serviceVersion(SecretServiceVersion.V7_1)
                    .credential(tokenCredential)
                    .buildClient();

            System.out.println("Setting a secret with name: " + secretName + ", value: " + secretValue);

            secretClient.setSecret(secretName, secretValue);

            System.out.println("Set the secret with name: " + secretName);


            //=============================================================
            // Get a secret.

            System.out.println("Getting the secret with name: " + secretName);

            secretClient.getSecret(secretName);

            System.out.println("Got the secret with name: " + secretName + ", value: " + secretValue);


            //=============================================================
            // Delete a key vault.

            System.out.println("Deleting key vault with name: " + vaultName);

            azureResourceManager.vaults().deleteById(vault.id());

            System.out.println("Deleting key vault with name: " + vaultName);
            return true;
        } finally {

            try {
                System.out.println("Deleting Resource Group: " + rgName);
                azureResourceManager.resourceGroups().beginDeleteByName(rgName);
            } catch (NullPointerException npe) {
                System.out.println("Did not create any resources in Azure. No clean up is necessary");
            } catch (Exception g) {
                g.printStackTrace();
            }
        }
    }

    private static AzureEnvironment getAzureEnvironmentFromArmEndpoint(String armEndpoint) {
        // Create HTTP client and request
        HttpClient httpClient = HttpClient.createDefault();

        HttpRequest request = new HttpRequest(HttpMethod.GET,
                String.format("%s/metadata/endpoints?api-version=2019-10-01", armEndpoint))
                .setHeader("accept", "application/json");

        // Execute the request and read the response
        HttpResponse response = httpClient.send(request).block();
        if (response.getStatusCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.getStatusCode());
        }
        String body = response.getBodyAsString().block();
        try {
            ArrayNode metadataArray = JacksonAdapter.createDefaultSerializerAdapter()
                    .deserialize(body, ArrayNode.class, SerializerEncoding.JSON);

            if (metadataArray == null || metadataArray.isEmpty()) {
                throw new RuntimeException("Failed to find metadata : " + body);
            }

            JsonNode metadata = metadataArray.iterator().next();
            AzureEnvironment azureEnvironment = new AzureEnvironment(new HashMap<String, String>() {
                {
                    put("managementEndpointUrl", metadata.at("/authentication/audiences/0").asText());
                    put("resourceManagerEndpointUrl", armEndpoint);
                    put("galleryEndpointUrl", metadata.at("/gallery").asText());
                    put("activeDirectoryEndpointUrl", metadata.at("/authentication/loginEndpoint").asText());
                    put("activeDirectoryResourceId", metadata.at("/authentication/audiences/0").asText());
                    put("activeDirectoryGraphResourceId", metadata.at("/graph").asText());
                    put("storageEndpointSuffix", "." + metadata.at("/suffixes/storage").asText());
                    put("keyVaultDnsSuffix", "." + metadata.at("/suffixes/keyVaultDns").asText());
                }
            });
            return azureEnvironment;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Main entry point.
     * @param args the parameters
     */
    public static void main(String[] args) {
        try {

            //=============================================================
            // Authenticate

            final FileInputStream configFileStream = new FileInputStream("../azureSecretSpConfig.json");

            final ObjectNode settings = JacksonAdapter.createDefaultSerializerAdapter()
                    .deserialize(configFileStream, ObjectNode.class, SerializerEncoding.JSON);

            final String clientId = settings.get("clientId").asText();
            final String clientSecret = settings.get("clientSecret").asText();
            final String clientObjectId = settings.get("clientObjectId").asText();
            final String subscriptionId = settings.get("subscriptionId").asText();
            final String tenantId = settings.get("tenantId").asText();
            final String armEndpoint = settings.get("resourceManagerUrl").asText();
            final String location = settings.get("location").asText();

            // Register Azure Stack cloud environment
            final AzureProfile profile = new AzureProfile(getAzureEnvironmentFromArmEndpoint(armEndpoint));
            final TokenCredential credential = new ClientSecretCredentialBuilder()
                    .tenantId(tenantId)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
                    .build();

            AzureResourceManager azureResourceManager = AzureResourceManager
                    .configure()
                    .withLogLevel(HttpLogDetailLevel.BASIC)
                    .authenticate(credential, profile)
                    .withTenantId(tenantId)
                    .withSubscription(subscriptionId);

            // Print selected subscription
            System.out.println("Selected subscription: " + azureResourceManager.subscriptionId());

            runSample(azureResourceManager, location, credential, clientObjectId);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private ManageKeyvaultSecret() {
    }
}

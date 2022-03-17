/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.azure.resourcemanager.storage.samples;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.util.serializer.JacksonAdapter;
import com.azure.core.util.serializer.SerializerEncoding;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.samples.Utils;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.resourcemanager.storage.models.StorageAccountKey;
import com.azure.resourcemanager.storage.models.StorageAccountSkuType;
import com.azure.resourcemanager.storage.models.StorageAccounts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Azure Stack Storage sample for managing storage accounts -
 * - Create a storage account
 * - Get | regenerate storage account access keys
 * - Create another storage account
 * - List storage accounts
 * - Delete a storage account.
 */

public final class ManageStorageAccount {
    /**
     * Main function which runs the actual sample.
     *
     * @param azureResourceManager instance of the azure client
     * @param location the Azure location
     * @return true if sample runs successfully
     */
    public static boolean runSample(AzureResourceManager azureResourceManager, String location) {
        final String storageAccountName = Utils.randomResourceName(azureResourceManager, "sa", 8);
        final String storageAccountName2 = Utils.randomResourceName(azureResourceManager, "sa2", 8);
        final String rgName = Utils.randomResourceName(azureResourceManager, "rgSTMS", 8);
        try {

            // ============================================================
            // Create a storage account

            System.out.println("Creating a Storage Account");

            StorageAccount storageAccount = azureResourceManager.storageAccounts().define(storageAccountName)
                    .withRegion(location)
                    .withNewResourceGroup(rgName)
                    .withGeneralPurposeAccountKind()
                    .withSku(StorageAccountSkuType.STANDARD_LRS)
                    .create();

            System.out.println("Created a Storage Account:");
            Utils.print(storageAccount);


            // ============================================================
            // Get | regenerate storage account access keys

            System.out.println("Getting storage account access keys");

            List<StorageAccountKey> storageAccountKeys = storageAccount.getKeys();

            Utils.print(storageAccountKeys);

            System.out.println("Regenerating first storage account access key");

            storageAccountKeys = storageAccount.regenerateKey(storageAccountKeys.get(0).keyName());

            Utils.print(storageAccountKeys);


            // ============================================================
            // Create another storage account

            System.out.println("Creating a 2nd Storage Account");

            StorageAccount storageAccount2 = azureResourceManager.storageAccounts().define(storageAccountName2)
                    .withRegion(location)
                    .withNewResourceGroup(rgName)
                    .withGeneralPurposeAccountKind()
                    .withSku(StorageAccountSkuType.STANDARD_LRS)
                    .create();

            System.out.println("Created a Storage Account:");
            Utils.print(storageAccount2);

            // ============================================================
            // List storage accounts

            System.out.println("Listing storage accounts");

            StorageAccounts storageAccounts = azureResourceManager.storageAccounts();

            PagedIterable<StorageAccount> accounts = storageAccounts.listByResourceGroup(rgName);
            for (StorageAccount sa : accounts) {
                System.out.println("Storage Account " + sa.name()
                        + " created @ " + sa.creationTime());
            }

            // ============================================================
            // Delete a storage account

            System.out.println("Deleting a storage account - " + storageAccount.name()
                    + " created @ " + storageAccount.creationTime());

            azureResourceManager.storageAccounts().deleteById(storageAccount.id());

            System.out.println("Deleted storage account");
            return true;
        } finally {
            try {
                System.out.println("Deleting Resource Group: " + rgName);
                azureResourceManager.resourceGroups().beginDeleteByName(rgName);
                System.out.println("Deleted Resource Group: " + rgName);
            } catch (Exception e) {
                System.out.println("Did not create any resources in Azure. No clean up is necessary");
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

            runSample(azureResourceManager, location);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private ManageStorageAccount() {

    }
}

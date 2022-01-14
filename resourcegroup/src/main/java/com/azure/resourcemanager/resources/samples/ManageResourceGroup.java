/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.azure.resourcemanager.resources.samples;

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
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.samples.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Azure Stack Resource sample for managing resource groups -
 * - Create a resource group
 * - Update a resource group
 * - Create another resource group
 * - List resource groups
 * - Delete a resource group.
 */

public final class ManageResourceGroup {
    /**
     * Main function which runs the actual sample.
     *
     * @param azureResourceManager instance of the azure client
     * @param location the Azure location
     * @return true if sample runs successfully
     */
    public static boolean runSample(AzureResourceManager azureResourceManager, String location) {
        final String rgName = Utils.randomResourceName(azureResourceManager, "rgRSMA", 24);
        final String rgName2 = Utils.randomResourceName(azureResourceManager, "rgRSMA", 24);
        final String resourceTagName = Utils.randomResourceName(azureResourceManager, "rgRSTN", 24);
        final String resourceTagValue = Utils.randomResourceName(azureResourceManager, "rgRSTV", 24);
        try {


            //=============================================================
            // Create resource group.

            System.out.println("Creating a resource group with name: " + rgName);

            ResourceGroup resourceGroup = azureResourceManager.resourceGroups().define(rgName)
                    .withRegion(location)
                    .create();

            System.out.println("Created a resource group with name: " + rgName);


            //=============================================================
            // Update the resource group.

            System.out.println("Updating the resource group with name: " + rgName);

            resourceGroup.update()
                    .withTag(resourceTagName, resourceTagValue)
                    .apply();

            System.out.println("Updated the resource group with name: " + rgName);


            //=============================================================
            // Create another resource group.

            System.out.println("Creating another resource group with name: " + rgName2);

            azureResourceManager.resourceGroups().define(rgName2)
                    .withRegion(location)
                    .create();

            System.out.println("Created another resource group with name: " + rgName2);


            //=============================================================
            // List resource groups.

            System.out.println("Listing all resource groups");

            for (ResourceGroup rGroup : azureResourceManager.resourceGroups().list()) {
                System.out.println("Resource group: " + rGroup.name());
            }


            //=============================================================
            // Delete a resource group.

            System.out.println("Deleting resource group: " + rgName2);

            azureResourceManager.resourceGroups().beginDeleteByName(rgName2);
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

            final FileInputStream configFileStream = new FileInputStream("../azureAppSpConfig.json");

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
}

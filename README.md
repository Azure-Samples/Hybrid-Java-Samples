---
page_type: sample
languages:
- java
products:
- azure
description: "Azure Stack Resource sample for managing resource groups."
urlFragment: Hybrid-resources-java-manage-resource-group
---

# Hybrid-Resources-Java-Manage-ResourceGroup


  Azure Stack Resource sample for managing resource groups -
  - Create a resource group
  - Update a resource group
  - Create another resource group
  - List resource groups
  - Delete a resource group.
 

## Running this Sample

To run this sample:

1. Clone the repository using the following command:

    git clone https://github.com/Azure-Samples/Hybrid-resources-java-manage-resource-group.git

2. Create an Azure service principal and assign a role to access the subscription. For instructions on creating a service principal in Azure Stack, see [Use Azure PowerShell to create a service principal with a certificate](https://docs.microsoft.com/en-us/azure/azure-stack/azure-stack-create-service-principals).

3. Set the following required environment variable values:

    * AZURE_TENANT_ID

    * AZURE_CLIENT_ID

    * AZURE_CLIENT_SECRET

    * AZURE_SUBSCRIPTION_ID

    * ARM_ENDPOINT

    * RESOURCE_LOCATION

4. Change directory to sample:
    
    * cd Hybrid-resources-java-manage-resource-group

5. Run the sample:
    * mvn clean compile exec:java

## More information

[http://azure.com/java](http://azure.com/java)

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

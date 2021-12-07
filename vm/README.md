---
page_type: sample
languages:
- java
products:
- azure
description: "Azure Compute sample for managing virtual machines"
urlFragment: hybrid-compute-java-manage-vm
---

# Hybrid-Compute-Java-Manage-VM


  Azure Compute sample for managing virtual machines -
   - Create a virtual machine
   - Update a virtual machine
     - Tag a virtual machine 
     - Attach data disks
     - Detach data disks
   - Restart a virtual machine
   - Stop(Poweroff) virtual machine
   - Delete a virtual machine.


## Running this Sample

To run this sample:

1. Clone the repository using the following command:
```bash
git clone https://github.com/Azure-Samples/hybrid-compute-java-manage-vm.git
```

2. Create an Azure service principal and assign a role to access the subscription. For instructions on creating a service principal, see [Use Azure PowerShell to create a service principal with a certificate](https://docs.microsoft.com/en-us/azure/azure-stack/azure-stack-create-service-principals).

3. Set the following required environment variable values:

    * `AZURE_TENANT_ID`

    * `AZURE_CLIENT_ID`

    * `AZURE_CLIENT_SECRET`

    * `AZURE_SUBSCRIPTION_ID`

    * `ARM_ENDPOINT`

    * `RESOURCE_LOCATION`

4. Change directory to Hybrid sample:
```bash
cd hybrid-compute-java-manage-vm
```

5. Run the sample:
```bash
mvn clean compile exec:java
```

## More information

[http://azure.com/java](http://azure.com/java)

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

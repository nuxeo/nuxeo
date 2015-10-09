This addon implements a BinaryManager that stores binaries in an Azure container.
For efficiency, a local disk cache (with limited size) is also used.

Be sure to protect your user id and token using the [configuration data encryption](https://doc.nuxeo.com/x/4YeRAQ).

# Mandatory parameters

- nuxeo.core.binarymanager=org.nuxeo.ecm.core.storage.azure.AzureBinaryManager

- nuxeo.storage.azure.container : the name of the Azure container

- nuxeo.storage.azure.account.name : your Azure storage account name

- nuxeo.storage.azure.account.key : your Azure storage access key

# Optional parameters

- nuxeo.storage.azure.endpointProtocol : the url protocol (default is HTTPS)

- nuxeo.storage.azure.cachesize : size of the local cache (default is 100MB).

# Building

    mvn clean install

## Deploying

Install [the Amazon Azure Storage Marketplace Package](https://connect.nuxeo.com/nuxeo/site/marketplace/package/azure-storage).
Or manually copy the built artifacts into `$NUXEO_HOME/templates/custom/bundles/` and activate the "custom" template with .

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Netflix, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.

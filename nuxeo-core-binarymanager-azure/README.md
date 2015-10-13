This addon implements a BinaryManager that stores binaries in an Azure container.
For efficiency, a local disk cache (with limited size) is also used.

Be sure to protect your user id and token using the [configuration data encryption](https://doc.nuxeo.com/x/4YeRAQ).

# Configuration

Configuration you have to add in your `nuxeo.conf` file:

## Enable default Azure Binary Manager

`nuxeo.core.binarymanager=org.nuxeo.ecm.blob.azure.AzureBinaryManager`

## Enable CDN Azure Binary Manager

See the [Microsoft Azure documentation page](https://azure.microsoft.com/en-us/documentation/articles/cdn-overview/) about how you can enable the CDN feature.

`nuxeo.core.binarymanager=org.nuxeo.ecm.blob.azure.AzureCDNBinaryManager`

## Mandatory parameters

- nuxeo.storage.azure.container : the name of the Azure container

- nuxeo.storage.azure.account.name : your Azure storage account name

- nuxeo.storage.azure.account.key : your Azure storage access key (Do not forget to use [data encryption](https://doc.nuxeo.com/x/4YeRAQ))

## Optional parameters

- nuxeo.storage.azure.endpointProtocol : the url protocol (default is `HTTPS`)

- nuxeo.storage.azure.cachesize : size of the local cache (default is `100MB`).

- nuxeo.storage.azure.directdownload : enable direct download from Azure servers (default is `false`)

- nuxeo.storage.azure.cdn.host : *(only if you enable direct download and use the Azure CDN)* your Azure CDN host where your blobs are available.

# Building

    mvn clean install

## Deploying

Install [the Amazon Azure Storage Marketplace Package](https://connect.nuxeo.com/nuxeo/site/marketplace/package/azure-storage).
Or manually copy the built artifacts into `$NUXEO_HOME/templates/custom/bundles/` and activate the "custom" template with .

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Netflix, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.

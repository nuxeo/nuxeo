# Nuxeo Azure Blob Storage

This addon implements a BinaryManager that stores binaries in an Azure container.
For efficiency, a local disk cache (with limited size) is also used.

## Prerequisites

You should be familiar with Azure and their Management Portal. Alos, you have to create a dedicated Azure blob Container; and be in possession of your Storage access keys.

## Configuration

Be sure to protect your access keys using the [configuration data encryption](https://doc.nuxeo.com/x/4YeRAQ).

Configuration properties you have to set in your `nuxeo.conf` file:

### Enable Azure Binary Manager

Setting up the default BinaryManager that stores all your blobs in Azure:

`nuxeo.core.binarymanager=org.nuxeo.ecm.blob.azure.AzureBinaryManager`

### Enable CDN Azure Binary Manager

If you want to use Azure CDN as a front instead of Storage; you should read  [Microsoft Azure documentation page](https://azure.microsoft.com/en-us/documentation/articles/cdn-overview/) and create a CDN that is binding to your container. Then, setting the corresponding BinaryManager:

`nuxeo.core.binarymanager=org.nuxeo.ecm.blob.azure.AzureCDNBinaryManager`

### Mandatory parameters

- nuxeo.storage.azure.container : the name of the Azure container

- nuxeo.storage.azure.account.name : your Azure storage account name

- nuxeo.storage.azure.account.key : your Azure storage access key (Do not forget to use [data encryption](https://doc.nuxeo.com/x/4YeRAQ))

### Optional parameters

- nuxeo.storage.azure.prefix : the directory prefix to use inside the container

- nuxeo.storage.azure.endpointProtocol : the url protocol (default is `HTTPS`)

- nuxeo.storage.azure.cachesize : size of the local cache (default is `100MB`).

- nuxeo.storage.azure.directdownload : enable direct download from Azure servers (default is `false`)

- nuxeo.storage.azure.cdn.host : *(only if you enable direct download and use the Azure CDN)* your Azure CDN host where your blobs are available.

## Install Marketplace Package

Look at the `microsoft-azure-online-storage` package from the Admin Center, or download [the Microsoft Azure Storage Marketplace Package](https://connect.nuxeo.com/nuxeo/site/marketplace/package/microsoft-azure-online-storage).

## Building from sources

    mvn clean install

Then, manually copy the `azure-storage-*.jar` lib into `$NUXEO_HOME/lib/`, `nuxeo-core-binarymanager-azure-*.jar` and `nuxeo-core-binarymanager-common-*.jar` built artifacts into `$NUXEO_HOME/nxserver/bundles/`.

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.

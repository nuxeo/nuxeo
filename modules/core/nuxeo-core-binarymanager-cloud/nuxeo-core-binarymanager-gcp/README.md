This addon implements a BinaryManager that stores binaries in a Google bucket.

Be sure to protect your nuxeo.conf (readable only by the nuxeo user) as the
file will have your Google identifiers.

# Mandatory parameters

- `nuxeo.core.binarymanager=org.nuxeo.ecm.core.storage.gcp.GoogleStorageBinaryManager`

- `nuxeo.gcp.storage.bucket`: the name of the Google bucket (that will be fetched or created)

- `nuxeo.gcp.storage.bucket_prefix`: the bucket prefix

- `nuxeo.gcp.project`: your GCP project

- `nuxeo.gcp.credentials`:
    - absolute JSON GCP credentials file path
    - file name of credentials in `nxserver/config`
    - if not set Nuxeo will look into 'gcp-credentials.json' file by default (located in `nxserver/config`)

# Building

`mvn clean install`

# Deploying

Install [the Google Storage Marketplace Package](https://connect.nuxeo.com/nuxeo/site/marketplace/package/google-storage).

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.

This addon implements a BinaryManager that stores binaries in several Cloud blob based provider like Amazon S3.
For efficiency, a local disk cache (with limited size) is also used.

# Modules

- nuxeo-core-binarymanager-s3: Amazon S3 implementation
- nuxeo-core-binarymanager-jclouds: Apache jclouds Binary Manager implementation (OLD)

# Building all modules

    mvn clean install

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Netflix, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.

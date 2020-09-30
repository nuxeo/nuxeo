
# Nuxeo Platform

[![Build Status](https://jenkins.platform.dev.nuxeo.com/buildStatus/icon?job=nuxeo/nuxeo/master)](https://jenkins.platform.dev.nuxeo.com/job/nuxeo/job/nuxeo/job/master/)

## About the Nuxeo Platform

The [Nuxeo Platform](http://www.nuxeo.com/products/content-management-platform/) is an open source customizable and extensible content management platform for building business applications. It provides the foundation for developing [document management](http://www.nuxeo.com/solutions/document-management/), [digital asset management](http://www.nuxeo.com/solutions/digital-asset-management/), [case management application](http://www.nuxeo.com/solutions/case-management/) and [knowledge management](http://www.nuxeo.com/solutions/advanced-knowledge-base/). You can easily add features using ready-to-use addons or by extending the platform using its extension point system.

The Nuxeo Platform is developed and supported by Nuxeo, with contributions from the community.

## Installation

Please follow the [Installation](https://doc.nuxeo.com/n/b1j) guide.

Running the Nuxeo Platform requires JDK 11, Oracle's JDK or OpenJDK.

Depending on the features you want to use, you may need some third-party software, such as LibreOffice and pdftohtml for document preview or ImageMagick for pictures. The list of third-party software is available in our Admin documentation: [Installing and Setting Up Related Software](https://doc.nuxeo.com/n/Yki).

## Building

Building the Nuxeo Platform requires the following tools:

- Git (obviously)
- JDK 11 (Oracle's JDK or OpenJDK recommended)
- Apache Maven 3.1.1+ (3.2+ recommended)
- Apache Ant 1.7.1+

Get the source code:

```shell
git clone git@github.com:nuxeo/nuxeo.git
cd nuxeo
```

Export the relevant options for the JVM running Maven:

```shell
export MAVEN_OPTS='-Xmx4g -Xms2g -XX:+TieredCompilation -XX:TieredStopAtLevel=1'
```

To build everything, including the packages, server ZIP and Docker image, run:

```shell
mvn install -Pdistrib,docker -DskipTests -Dnuxeo.skip.enforcer=true -T6
```

To take the shortest path for building the Docker image, run the following command. It starts by building the Docker image's dependencies (server modules and ZIP), then the Docker image itself.

```shell
mvn install -Pdistrib,docker -pl docker -am -DskipTests -Dnuxeo.skip.enforcer=true -T6
```

See our [Nuxeo Core Developer Guide](https://doc.nuxeo.com/n/9ib) for complete instructions and guidelines.

## Resources

### Documentation

The documentation for the Nuxeo Platform is available in our [Documentation Center](http://doc.nuxeo.com):

- [Developer Documentation](https://doc.nuxeo.com/nxdoc/next/)
- [Server Documentation](https://doc.nuxeo.com/n/aac)
- [REST API Documentation](https://doc.nuxeo.com/rest-api/1/)
- [User Documentation](https://doc.nuxeo.com/n/pvr)
- [Nuxeo Core Developer Guide](https://doc.nuxeo.com/n/9ib)

### Benchmarks

The Nuxeo Platform is [benchmarked continuously](https://benchmarks.nuxeo.com/) to test its massive scalability on several databases.

### Reporting Issues

You can follow the developments in the [Nuxeo Platform](https://jira.nuxeo.com/browse/NXP/) project of our JIRA bug tracker.

You can report issues on [answers.nuxeo.com](http://answers.nuxeo.com).

## Licensing

Most of the source code in the Nuxeo Platform is copyright Nuxeo and
contributors, and licensed under the Apache License, Version 2.0.

See the [LICENSE](LICENSE) file and the documentation page [Licenses](https://doc.nuxeo.com/n/o_J) for details.

## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at [www.nuxeo.com](http://www.nuxeo.com).

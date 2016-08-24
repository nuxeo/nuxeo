
| Nuxeo QA Job | Status | 
|-----|--------|
| Build & Unit tests | [![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=master/nuxeo-master)](https://qa.nuxeo.org/jenkins/job/master/job/nuxeo-master)|
| WebDriver functional testing | [![Test Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=master/FT-nuxeo-master-webdriver-cap-tomcat)](https://qa.nuxeo.org/jenkins/job/master/job/FT-nuxeo-master-webdriver-cap-tomcat)
|Funkload tests with multi-DB |[![Test Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=master/FT-nuxeo-master-funkload-cap-tomcat-multidb)](https://qa.nuxeo.org/jenkins/job/master/job/FT-nuxeo-master-funkload-cap-tomcat-multidb)


# About the Nuxeo Platform


The [Nuxeo Platform](http://www.nuxeo.com/products/content-management-platform/) is an open source customizable and extensible content management platform for building business applications. It provides the foundation for developing [document management](http://www.nuxeo.com/solutions/document-management/), [digital asset management](http://www.nuxeo.com/solutions/digital-asset-management/), [case management application](http://www.nuxeo.com/solutions/case-management/) and [knowledge management](http://www.nuxeo.com/solutions/advanced-knowledge-base/). You can easily add features using ready-to-use addons or by extending the platform using its extension point system.

The Nuxeo Platform is developed and supported by Nuxeo, with contributions from the community.

# Sub-Modules Organization

The project is splitted in several sub-modules (listed in dependency order):

* **nuxeo-common**:
Common utilities
* **nuxeo-runtime**:
Container and runtime basic services
* **nuxeo-core**:
Document/content management core services
* **nuxeo-services**:
Basic services such as file manager, directories, document types
* **nuxeo-theme**:
Services related to the theme and theme rendering
* **nuxeo-jsf**:
JSF related services
* **nuxeo-webengine**:
Services and framework related to WebEngine, the Nuxeo lighweight rendering engine
* **nuxeo-features**:
Advanced high-level services, such as audit, imaging, publisher, thumbnails, search
* **nuxeo-dm**:
The default Nuxeo Platform application, mostly configuration and UI elements
* **nuxeo-distribution**:
This module builds, packages and tests the Nuxeo products.

# Building

## Requirements

Running the Nuxeo Platform requires Java 8.
Depending on the features you want to use, you may need some third-party software, such as Libre Office and pdftohtml for document preview or ImageMagick for pictures. The list of third-party software is available in our Admin documentation: [Installing and Setting Up Related Software](http://doc.nuxeo.com/x/zgJc).

Building the Nuxeo Platform requires the following tools:

* JDK 8 (Oracle's JDK or OpenJDK recommended)
* Apache Maven 3.1.1+ (3.2+ recommended)
* Apache Ant 1.7.1+
* Git (obviously)
* NodeJS 0.10.32, npm, yo, grunt-cli, gulp, bower

# QA

Each module includes unit and integration tests. Functional tests are available in nuxeo-distribution and for each addon package module.

We also provide some tooling for tests:

* [https://github.com/nuxeo/tools-nuxeo-ftest](https://github.com/nuxeo/tools-nuxeo-ftest)
* [https://github.com/nuxeo/ant-assembly-maven-plugin](https://github.com/nuxeo/ant-assembly-maven-plugin)
* [https://github.com/nuxeo/integration-scripts/](https://github.com/nuxeo/integration-scripts/)

# Deploying

1. Get the source code:
```
git clone git@github.com:nuxeo/nuxeo.git
cd nuxeo
python clone.py master -a
```
2. Build using Maven:
```
mvn clean install -Paddons,distrib
```

See our [Core Developer Guide](http://doc.nuxeo.com/x/B4BH) for instructions and guidelines.

# Resources

## Documentation

The documentation for the Nuxeo Platform is available in our [Documentation Center](http://doc.nuxeo.com):

* Developer documentation: [http://doc.nuxeo.com/x/PIAO](http://doc.nuxeo.com/x/PIAO)
* Admin documentation: [http://doc.nuxeo.com/x/G4AO](http://doc.nuxeo.com/x/G4AO)
* User documentation: [http://doc.nuxeo.com/x/6ICo](http://doc.nuxeo.com/x/6ICo)
* Core Developer Guide: [http://doc.nuxeo.com/x/B4BH](http://doc.nuxeo.com/x/B4BH)

## QA results

Follow the status of the Nuxeo Platform continuous integration build on our QA platform: [https://qa.nuxeo.org/jenkins](https://qa.nuxeo.org/jenkins)

## Reporting issues

You can follow the developments in the Nuxeo Platform project of our JIRA bug tracker: [https://jira.nuxeo.com/browse/NXP/](https://jira.nuxeo.com/browse/NXP/).

You can report issues on [answers.nuxeo.com](http://answers.nuxeo.com).

# Licensing

Most of the source code in the Nuxeo Platform is copyright Nuxeo SA and
contributors, and licensed under the GNU Lesser General Public License v2.1.

See [/licenses](/licenses) and the documentation page [Licenses](http://doc.nuxeo.com/x/gIK7) for details.

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at [www.nuxeo.com](http://www.nuxeo.com).


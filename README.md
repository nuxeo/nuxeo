# Nuxeo Drive Server

Addon needed for [Nuxeo Drive](https://github.com/nuxeo/nuxeo-drive) to work against a Nuxeo Platform instance.

# Building

    mvn clean install

## Deploying

Install [the Nuxeo Drive Marketplace Package](https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-drive).
Or manually copy the built artifacts into `$NUXEO_HOME/templates/custom/bundles/` and activate the "custom" template.

You should then have the 'Nuxeo Drive' tab in your Home allowing you to download the Nuxeo Drive client for your favorite OS :-)

## QA results

| Nuxeo QA Job | Status |
|-----|--------|
| Build Status | [![Build Status](https://qa.nuxeo.org/jenkins/job/master/job/addons_nuxeo-drive-master-marketplace/badge/icon)](https://qa.nuxeo.org/jenkins/job/master/job/addons_nuxeo-drive-master-marketplace/) |
| Build Multi-DB | [![Build Multi-DB](https://qa.nuxeo.org/jenkins/job/master/job/addons_nuxeo-drive-server-master-multidb/badge/icon)](https://qa.nuxeo.org/jenkins/job/master/job/addons_nuxeo-drive-server-master-multidb) |

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.

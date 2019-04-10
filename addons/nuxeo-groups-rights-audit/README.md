# Nuxeo group rights audit

Get an Excel Export of groups content (subgroups/users)

- Provides Export Groups service/action/operation to get the blob (action in admin center Users&Groups)
- Provides Export Listed Groups service/action to get in the admin center the listed groups definition in an excel report (action in admin center Users&Groups)
- Provides excel export extension point to contribute an Excel template and injected data to get an Excel Export

# Building

    mvn clean install

## Deploying

Install [the Nuxeo Groups and Rights Audit Marketplace Package](https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-groups-rights-audit).
Or manually copy the built artifacts into `$NUXEO_HOME/templates/custom/bundles/` and activate the "custom" template.

## QA results

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_nuxeo-groups-rights-audit-master)](https://qa.nuxeo.org/jenkins/job/addons_nuxeo-groups-rights-audit-master/)

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.

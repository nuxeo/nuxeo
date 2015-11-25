# Nuxeo Platform Layout Demo

This addon presents layouts and widgets in a "show-case like" web site.

The application is visible at http://localhost:8080/nuxeo/layoutDemo 

Webdriver tests also ensure that all standard widget types are working ok.

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_nuxeo-platform-forms-layout-demo-master)](https://qa.nuxeo.org/jenkins/job/addons_nuxeo-platform-forms-layout-demo-master/)
[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_FT_nuxeo-platform-forms-layout-demo-master-webdriver)](https://qa.nuxeo.org/jenkins/job/addons_FT_nuxeo-platform-forms-layout-demo-master-webdriver/)
[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_FT_nuxeo-platform-forms-layout-demo-master-webdriver-list-widget)](https://qa.nuxeo.org/jenkins/job/addons_FT_nuxeo-platform-forms-layout-demo-master-webdriver-list-widget/)

## Building

    mvn clean install

## Deploying

1. Put generated jar nuxeo-platform-forms-layout-demo to your
   nxserver/bundles directory
2. Start the server.

## Launching webdriver tests

Run:

    $ mvn clean install -Pitest


# Development

## Adding a new widget type
To add a new widget type to the application, you need to:
- add a xhtml template in nuxeo.war/layoutDemo/demoWidgets
- declare this template with a view id in OSGI-INF/deployment-fragment.xml
- provide the widget sample xml configuration in OSGI-INF/demo and
  reference it in the MANIFEST
- configure the widget type in OSGI-INF/layout-demo-contrib.xml

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Netflix, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
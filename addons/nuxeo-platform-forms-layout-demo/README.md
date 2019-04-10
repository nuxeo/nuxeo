# Nuxeo Platform Layout Demo

This addon presents layouts and widgets in a "show-case like" web site.

The application is visible at http://localhost:8080/nuxeo/layoutDemo

Webdriver tests also ensure that all standard widget types are working ok.

Unit Tests: [![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_nuxeo-platform-forms-layout-demo-master)](https://qa.nuxeo.org/jenkins/job/addons_nuxeo-platform-forms-layout-demo-master/)

Webdriver Tests: [![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_FT_nuxeo-platform-forms-layout-demo-master-webdriver)](https://qa.nuxeo.org/jenkins/job/addons_FT_nuxeo-platform-forms-layout-demo-master-webdriver/)

Webdriver List Widget Compat Tests: [![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_FT_nuxeo-platform-forms-layout-demo-master-webdriver-list-widget)](https://qa.nuxeo.org/jenkins/job/addons_FT_nuxeo-platform-forms-layout-demo-master-webdriver-list-widget/)

The application presents standard widget types available to the Nuxeo layout service, and can be used as a demo, reference and preview application.

You can browse widget types by clicking on items in the left menu.

The idea of this application came with [Studio integration](http://www.nuxeo.com/en/products/studio), as widget types configuration needed to be made user friendly by:

* providing a library of available widget types, taking care of the widget accepted field type (String, Boolean, Integer....);
* making it possible to set the widget properties using a form;
* making it possible to preview the resulting widget.

It was also interesting for functional tests, to verify that default widget types all work as expected, and are styled correctly.

So the widget type definition was improved to hold configuration
instructions (title, description, definition of field types that match
the widget type...).

To describe the available properties depending on the mode, as well as
the form to display them, layouts were the natural choice. So the widget
type definition was modified to accept standard layout definitions, with
widgets using the property name as a field.

Using this way of describing a widget type is also a good maintenance tool:
the information is kept in XML format, accepting HTML rendering for the
description. It can be used directly to generate pages like the
reference page or preview forms on this showcase application (using the
standard JSF implementation of layouts).

It can be exported in JSON format for Nuxeo Studio, as it accepts
widget types as JSON contributions and uses them to fill its registries.
A specific GWT implementation of layouts has been implemented for this
purpose.

## Building

    mvn clean install

## Deploying

1. Put generated jar nuxeo-platform-forms-layout-demo to your
   nxserver/bundles directory
2. Start the server.

## Launching Webdriver Tests

    mvn clean install -Pitest


# Development

## Adding a New Widget Type

To add a new widget type to the application, you need to:
- add a xhtml template in nuxeo.war/layoutDemo/demoWidgets
- declare this template with a view id in OSGI-INF/deployment-fragment.xml
- provide the widget sample xml configuration in OSGI-INF/demo and
  reference it in the MANIFEST
- configure the widget type in OSGI-INF/layout-demo-contrib.xml

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
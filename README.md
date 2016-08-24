# Nuxeo Platform Suggest Box

This repository hosts the source code of a Nuxeo service to suggest actions
(e.g. navigate to a document or user profile page or perform a filtered
search for documents) from a contextual user text input (e.g. search box).

This repository also provides a Seam component and JSF template to override
the top right search box of the default Nuxeo web interface to provide
AJAX suggestions.

In the future the service suggestion feature could also be exposed as
a Nuxeo Content Automation operation to build similar UI for clients
using the Android or iOS SDKs for instance.

# Building

    mvn clean install

## Deploying

Copy the built artifacts into `$NUXEO_HOME/templates/custom/bundles/` and activate the "custom" template.

## QA results

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_nuxeo-platform-suggestbox-master)](https://qa.nuxeo.org/jenkins/job/addons_nuxeo-platform-suggestbox-master/)

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.

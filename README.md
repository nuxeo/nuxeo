# Nuxeo Target Platforms

This module allows to declare definitions of Nuxeo target platforms
and packages.

When these XML contributions are deployed on the service, a tab is
made available in the Admin Center to make it possible to override
some of the target platforms attribute, on a live instance.

It also provides helpers to extend target platforms, packages,
etc.. classes to add specific information to these targets. This is
useful for Connect, IO and Studio features.

## Modules

- nuxeo-target-platforms-api: interfaces and default POJO
  implementations.
- nuxeo-target-platforms-core: runtime service implementation relying
  on XML contributions and service implementation, querying a dedicated
  directory for target platforms attributes override.
- nuxeo-target-platforms-jsf: pages for management of target platforms
  in the Admin Center.
- nuxeo-target-platforms-io: export of target platforms and packages
  information providing Json serialization.
- nuxeo-target-platforms-jaxrs: REST API for Json exports (see below)
- nuxeo-target-platforms-sample: sample contributions to the service.

## Build

    $ mvn clean install

## Install

Deploy all modules (expect maybe the sample one) to the
TOMCAT/nxserver/bundles directory.

## Usage

Admin center tab is available at Admin Center > Target Platforms.

## REST API

All calls accept boolean query parameter "pretty" for pretty print of
Json serialization.

### Json seralization for target platform 'cap-5.8'

    http://localhost:8080/nuxeo/site/target-platforms/platform/cap-5.8

or

    http://localhost:8080/nuxeo/site/target-platforms/platform/cap-5.8?pretty=true

(pretty printed)

### Json serialization for target platform info 'cap-5.8'

    http://localhost:8080/nuxeo/site/target-platforms/platform-info/cap-5.8:

### Json serialization for target package 'nuxeo-dm-5.8'

    http://localhost:8080/nuxeo/site/target-platforms/package/nuxeo-dm-5.8

### Json serialization for target package info 'nuxeo-dm-5.8'

    http://localhost:8080/nuxeo/site/target-platforms/package-info/nuxeo-dm-5.8

### Json serialization for target platform instance 'cap-5.8'

    http://localhost:8080/nuxeo/site/target-platforms/platform-instance/cap-5.8

(no packages activated)

    http://localhost:8080/nuxeo/site/target-platforms/platform-instance/cap-5.8?packages=nuxeo-dm-5.8

(package 'nuxeo-dm-5.8' activated)

    http://localhost:8080/nuxeo/site/target-platforms/platform-instance/cap-5.8?packages=nuxeo-dm-5.8,nuxeo-dam-5.8

(packages 'nuxeo-dm-5.8' and 'nuxeo-dam-5.8' activated)

### Json serialization of available target platforms

    http://localhost:8080/nuxeo/site/target-platforms/platforms

 (without filtering criteria)

    http://localhost:8080/nuxeo/site/target-platforms/platforms?filterDisabled=false&filterRestricted=false&filterDeprecated=false

 (with filtering criteria)

### Json serialization of available target platforms info

    http://localhost:8080/nuxeo/site/target-platforms/platforms-info

 (without filtering criteria)

    http://localhost:8080/nuxeo/site/target-platforms/platforms-info?filterDisabled=false&filterRestricted=false&filterDeprecated=false

 (with filtering criteria)

## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software
platform for enterprise content management] [1] and packaged
applications for [document management] [2], [digital asset management]
[3]. Designed by developers for developers, the Nuxeo platform offers
a modern architecture, a powerful plug-in model and extensive
packaging capabilities for building content applications.

[1]: http://www.nuxeo.com/en/products/ep
[2]: http://www.nuxeo.com/en/products/document-management
[3]: http://www.nuxeo.com/en/products/dam

More information at <http://www.nuxeo.com/>.
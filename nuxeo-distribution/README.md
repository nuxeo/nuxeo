# About the Nuxeo Distribution module

See <http://doc.nuxeo.org/> for full documentation.

This module builds, packages and tests the Nuxeo products.

## Requirements

Building Nuxeo products requires the following tools:

  * JDK 7 (Oracle's JDK or OpenJDK recommended)
  * Apache Maven 3.1.1+
  * Apache Ant 1.7.1+ (optional)
  * Open Source tools that will be downloaded by Maven when needed.

## Usage

### Build with Maven (recommended)

Java and Maven are at the lowest level, all configuration about building a module
is given in the Maven POM file.

Maven usage: `mvn clean package [options]`

#### Examples

 * Build all Nuxeo products without running Integration Tests:

        mvn clean package
        mvn clean verify -DskipTests
        mvn clean verify -DskipITs

 * Build all Nuxeo products running Integration Tests:

        mvn clean verify

 * Build only the Tomcat distributions

        mvn clean package -pl :nuxeo-distribution-tomcat

 * Build only the Tomcat CAP distribution (excluding Core Server)

        mvn clean package -pl :nuxeo-distribution-tomcat -Pnuxeo-cap

 * Run CAP Functional Tests after build of the needed resources:

        mvn clean verify -pl :nuxeo-distribution-cap-webdriver-tests -am

### Build with Ant (deprecated)

Ant is available at the top level: Ant targets have been defined to provide
user-friendly commands for most used build cases.

Ant usage: `ant distrib [-Ddistrib=profile]`

#### Examples

        ant distrib
        ant distrib -Ddistrib=nuxeo-dm
        ant distrib -Ddistrib=tomcat


### Available Maven profiles

 * sdk: build SDK distributions for use in Nuxeo IDE
 * qa, nightly: for internal use at Nuxeo

## Modules listing

 * nuxeo-distribution-cap: Content Application Platform NXR
 * nuxeo-distribution-cap/ftest/webdriver: WebDriver tests for CAP
 * nuxeo-distribution-coreserver: CoreServer NXR
 * nuxeo-distribution-dm: Document Management NXR
 * nuxeo-distribution-dm/ftest/cmis: CMIS tests for DM
 * nuxeo-distribution-dm/ftest/funkload: FunkLoad tests for DM
 * nuxeo-distribution-dm/ftest/selenium: Selenium tests for DM
 * nuxeo-distribution-dm/ftest/webdriver: WebDriver tests for DM
 * nuxeo-distribution-resources: Resources archives used in other packagings (doc, binaries, templates).
 * nuxeo-distribution-tests: Helper POM with Nuxeo test dependencies
 * nuxeo-distribution-tomcat: Tomcat distributions
 * nuxeo-functional-tests: Framework for testing nuxeo distributions
 * nuxeo-launcher: Control Panel and launcher
 * nuxeo-marketplace-dm: Marketplace Package of DM
 * nuxeo-startup-wizard: Startup Wizard WebApp
 * nuxeo-distribution-tomcat-wizard-tests: WebDriver tests for Tomcat wizard

## Produced packages

 * NXR packages
   * Core Server
   * Content Application Platform (CAP)
   * Advanced Document Management (DM)
   * Digital Assets Management (DAM)
   * Social Collaboration (SC)
 * Marketplace Packages
   * Advanced Document Management (DM)
   * Digital Assets Management (DAM)
   * Social Collaboration (SC)
 * Tomcat packages
   * Core Server
   * Content Application Platform (CAP)

## Understanding Maven phases and options

From (http://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html):
The default lifecycle has the following build phases (for a complete list of the build phases, refer to the [Lifecycle Reference](http://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html#Lifecycle_Reference)):

 * validate - validate the project is correct and all necessary information is available
 * compile - compile the source code of the project
 * test - test the compiled source code using a suitable unit testing framework. These tests should not require the code be packaged or deployed
 * package - take the compiled code and package it in its distributable format, such as a JAR.
 * integration-test - process and deploy the package if necessary into an environment where integration tests can be run
 * verify - run any checks to verify the package is valid and meets quality criteria
 * install - install the package into the local repository, for use as a dependency in other projects locally
 * deploy - done in an integration or release environment, copies the final package to the remote repository for sharing with other developers and projects.

See also Nuxeo Documentation: [CORG/Maven+usage](http://doc.nuxeo.com/x/JQk7)

## Details about predefined applications

### Nuxeo Core Server

A minimal server NXR. An embedded repository will be started. No other  platform services are available.

This application can be used to debug, test or develop nuxeo components that need a repository connection.

Remoting will be also available in the future via Nuxeo Runtime.

Built NXR is in `nuxeo-distribution-coreserver/target/`.

It is available within Tomcat in `nuxeo-distribution-tomcat/target/`.


### Nuxeo CAP

Basic document management features.

Built NXR is in `nuxeo-distribution-cap/target/`.

This is the default available application in `nuxeo-distribution-tomcat/target/`.

### Nuxeo Document Management

Advanced document management features.

Built NXR is in `nuxeo-distribution-dm/target/`.

It is installable in the default available application in `nuxeo-distribution-tomcat/target/` when running the wizard and selecting DM (for users), or by activating the "nuxeo-dm" preset (for developers).

### Other applications

There are a lot of other useful addons (see <http://nuxeo.github.io>) and applications packages available from the [Nuxeo Marketplace](http://marketplace.nuxeo.com/).

Addons are manually built and deployed on the target server.

Marketplace applications are installable from the Admin Center within the Nuxeo server.

## Custom build

It is of course possible to create custom builds/assemblies.

Multiple technologies have been used for packaging in the nuxeo-distribution project:
[maven-assembly-plugin](http://maven.apache.org/plugins/maven-assembly-plugin/), [maven-nuxeo-plugin](http://hg.nuxeo.org/tools/maven-nuxeo-plugin/), [maven-antrun-extended-plugin](http://java.net/projects/maven-antrun-extended-plugin), [nuxeo-distribution-tools](https://github.com/nuxeo/nuxeo-distribution-tools),
[ant-assembly-maven-plugin](https://github.com/nuxeo/ant-assembly-maven-plugin).

They are all based on Maven principles with the objectives to avoid duplication,
ease maintenance and upgrade, rely on Maven artifacts, be OS independent.

We recommend to use our newest tool [ant-assembly-maven-plugin](http://doc.nuxeo.com/x/BIAO).

Execution of the assembly is done from Maven execution as a Maven plugin.

Based on Ant syntax, it provides access to major Maven concepts and Ant flexibility.

Principles of an assembly are generally to:

  * inherit a Maven dependency tree/graph (list of artifacts to work with)
  * use this dependency tree to dispatch artifacts into directories
  * download complementary artifacts (default packaging, resources, drivers, ...)
  * download an empty server (JBoss, Jetty, Tomcat, ...)
  * assemble all those parts as a ZIP build product.

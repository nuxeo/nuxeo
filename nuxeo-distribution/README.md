# About the Nuxeo Distribution module

See <https://doc.nuxeo.com/> for full documentation.

This module builds, packages and tests the Nuxeo products.

## Requirements

Building Nuxeo products requires the following tools:

  * JDK 8 (Oracle's JDK or OpenJDK recommended)
  * Apache Maven 3.1.1+
  * Apache Ant 1.7.1+ (optional)
  * Open Source tools that will be downloaded by Maven when needed.

## Usage

### Build with Maven (recommended)

Java and Maven are at the lowest level, all configuration about building a module
is given in the Maven POM file.

Maven usage: `mvn clean package [options]`

#### Examples

 * Build all Nuxeo products without running Unit, Functional, Integration or Performance Tests:

        mvn clean package
        mvn clean verify -DskipTests -DskipITs

 * Build all Nuxeo products running Integration Tests:

        mvn clean verify

 * Build only the Tomcat distributions

        mvn clean package -pl :nuxeo-distribution-tomcat

 * Run JSF UI WebDriver Functional Tests after build of the needed resources:

        mvn clean verify -pl :nuxeo-jsf-ui-webdriver-tests -am

### Build with Ant (deprecated)

Ant usage: `ant package`

### Available Maven profiles

 * sdk: build SDK distributions for use in Nuxeo IDE
 * qa, nightly: for internal use at Nuxeo (daily, internal, nightly builds)
 * tomcat, pgsql, mssql, oracle10g, oracle11g, oracle12c, monitor, mongodb, bench, perf: for internal use at Nuxeo (testing)

## Modules listing

 * nuxeo-functional-tests: Framework for testing Nuxeo distributions
 * nuxeo-startup-wizard: Startup Wizard WebApp
 * nuxeo-launcher: Control Panel and Launcher
 * nuxeo-distribution-resources: Resources for packaging (doc, binaries, configuration templates)
 * nuxeo-distribution-tests: Convenient helper POM listing the Nuxeo test dependencies
 * nuxeo-distribution-server: Server NXR
 * nuxeo-distribution-cap: Content Application Platform NXR, **deprecated**
 * nuxeo-distribution-tomcat: Package Nuxeo Server with Tomcat
 * nuxeo-distribution-tomcat-wizard-tests: WebDriver tests on Startup Wizard
 * nuxeo-server-cmis-tests: CMIS tests on Nuxeo Server
 * nuxeo-jsf-ui-funkload-tests: Funkload tests and bench on Nuxeo JSF UI
 * nuxeo-jsf-ui-gatling-tests: Gatling bench on Nuxeo JSF UI
 * nuxeo-jsf-ui-webdriver-tests: WebDriver functional tests on Nuxeo JSF UI
 * nuxeo-marketplace-dm: Transitional package for DM

## Produced packages

 * NXR packages
   * Server
   * Content Application Platform (CAP), **deprecated**
 * Nuxeo Packages
   * Transitional Package for Advanced Document Management (DM)
 * Tomcat packages
   * Server
   * Content Application Platform (CAP), **deprecated**
   * SDK

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

### Nuxeo Server

A minimal server NXR. An embedded repository will be started. No other  platform services are available.

This application can be used to debug, test or develop nuxeo components that need a repository connection.

Built NXR is in `nuxeo-distribution-server/target/`.

This is the default application packaged within Tomcat in `nuxeo-distribution-tomcat/target/` (see "server").


### Nuxeo CAP

Basic document management features. **The package has been deprecated; it is kept for helping in transition.**

Built NXR is in `nuxeo-distribution-cap/target/`.

It is packaged within Tomcat in `nuxeo-distribution-tomcat/target/` (see "nuxeo-cap").

### Nuxeo Document Management

Advanced document management features. The package has been split and deprecated; it is kept for helping in transition.

Built Nuxeo Package is in `nuxeo-marketplace-dm/target/`.

It can be installed in CAP Tomcat using `nuxeoctl` or from the Administration page within the Nuxeo server.

### Other applications

There are a lot of other useful addons (see <http://nuxeo.github.io>) and packages available from the [Nuxeo Marketplace](http://marketplace.nuxeo.com/).

Nuxeo Addons are manually built and deployed on the target server.

Nuxeo Packages can be installed using `nuxeoctl` or from the Administration page within the server.

## Custom build

It is of course possible to create custom builds/assemblies.

### ant-assembly-maven-plugin

We recommend to use the [ant-assembly-maven-plugin](http://doc.nuxeo.com/x/BIAO).

Other technologies have been tested and used over time for packaging in the nuxeo-distribution project:
[maven-assembly-plugin](http://maven.apache.org/plugins/maven-assembly-plugin/), [maven-nuxeo-plugin](http://hg.nuxeo.org/tools/maven-nuxeo-plugin/), [maven-antrun-extended-plugin](http://java.net/projects/maven-antrun-extended-plugin), [nuxeo-distribution-tools](https://github.com/nuxeo/nuxeo-distribution-tools),
[ant-assembly-maven-plugin](https://github.com/nuxeo/ant-assembly-maven-plugin). They are all based on Maven principles with the objectives to avoid duplication, ease maintenance and upgrade, rely on Maven artifacts, be OS independent.

### assembly.xml

The execution of an assembly is managed by the Maven plugin during the build.

Based on Ant syntax, it provides access to major Maven concepts and Ant flexibility.

Principles of an assembly are generally to:

  * inherit a Maven dependency tree/graph (list of artifacts to work with)
  * use this dependency tree to dispatch artifacts into directories
  * download complementary artifacts (default packaging, resources, drivers...)
  * download an empty server (JBoss, Jetty, Tomcat...)
  * assemble all those parts as a ZIP build product.

Assemblies are also used to orchestrate the environment and server setup for testing.

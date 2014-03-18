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
is given in the Maven POM file. So, everything can be built using Maven but it
requires some knowledge about Nuxeo and its packagings.

Maven usage: `mvn clean package [-P<comma separated profiles>]`

#### Examples

 * (default) Building all Nuxeo products and their alternatives:

        mvn clean package
        mvn clean package -Pall-distributions

 * Only Tomcat packages:

        mvn clean package -Ptomcat

### Build with Ant (deprecated)

Ant is available at the top level: Ant targets have been defined to provide
user-friendly commands for most used build cases.

Ant usage: `ant distrib [-Ddistrib=profile]`

#### Examples

        ant distrib
        ant distrib -Ddistrib=nuxeo-dm
        ant distrib -Ddistrib=tomcat


### Available Maven profiles

 * all-distributions (default): full build
 * nuxeo-coreserver, nuxeo-cap, nuxeo-dm, nuxeo-dam, ...: build the corresponding application/module
 * tomcat: build the corresponding server
 * itest: run integration tests
 * ftest-dm, ftest-dam, ftest-sc, ...: run functional tests against the corresponding application/module
 * fltest-dm: run functional FunkLoad tests against DM
 * sdk: build SDK distributions for use in Nuxeo IDE
 * qa, nightly: for internal use at Nuxeo

## Modules listing

 * nuxeo-distribution-cap: Content Application Platform NXR
 * nuxeo-distribution-coreserver: CoreServer NXR
 * nuxeo-distribution-dm: Document Management NXR
    * ftest/cmis: CMIS tests for DM
    * ftest/funkload: FunkLoad tests for DM
    * ftest/selenium: Selenium tests for DM
    * ftest/webdriver: WebDriver tests for DM
 * nuxeo-distribution-resources: Resources archives used in other packagings (doc, binaries, templates).
 * nuxeo-distribution-social-collaboration: Social Collaboration NXR
    * ftest/selenium: Selenium tests for SC
 * nuxeo-distribution-tests: Helper POM with Nuxeo test dependencies
 * nuxeo-distribution-tomcat: Tomcat distributions
 * nuxeo-functional-tests: Framework for testing nuxeo distributions
 * nuxeo-dam-functional-tests: DAM specific additions to nuxeo-functional-tests
 * nuxeo-launcher: Control Panel and launcher
 * nuxeo-marketplace-dam: Marketplace package of DAM
    * ftest/webdriver: WebDriver tests for DAM
 * nuxeo-marketplace-dm: Marketplace package of DM
 * nuxeo-marketplace-rest-api: Marketplace package of REST API
 * nuxeo-marketplace-social-collaboration: Marketplace package of Social Collaboration
 * nuxeo-startup-wizard: Startup Wizard WebApp
 * nuxeo-distribution-tomcat-wizard-tests: WebDriver tests for Tomcat wizard

## Produced packages

 * NXR packages
     * Core Server
     * Content Application Platform (CAP)
     * Advanced Document Management (DM)
     * Digital Assets Management (DAM)
     * Social Collaboration (SC)
 * Marketplace packages
     * Advanced Document Management (DM)
     * Digital Assets Management (DAM)
     * Social Collaboration (SC)
     * REST API
 * Tomcat packages
     * Core Server
     * Content Application Platform (CAP)

## Understanding Maven profiles and classifiers

Profiles are mainly used to manage the list of classifiers being generated.
Maven plugins rely by default on a such mechanism for creating tests, sources and
Javadoc JARs. It is usable also for any other specific builds (OS, JDK, env, packaging, ...).
It's widely used by a lot of third-parties (Google GWT, JSON, shindings, ...).
Think about "classifiers" as "qualifiers" (sources, javadoc, tests, linux, windows,
mac, jta, all, ...). For example, the following are two alternatives ("classifiers")
for the package ("artifact") named "nuxeo-distribution-tomcat":
  * nuxeo-distribution-tomcat-5.4.0-SNAPSHOT-nuxeo-dm-jtajca.zip
  * nuxeo-distribution-tomcat-5.4.0-SNAPSHOT-nuxeo-dm.zip

Some profiles are used to choose the product to build. Other profiles are used to
choose which alternatives (classifiers) of the product will be built. Multiple
profiles can be used simultaneously.
Here are some common profiles and their impact on build result:
  * all-distributions: build everything
  * all: build all classifiers for the called module(s)
  * nuxeo-cap: build only Nuxeo CAP related modules stack
  * nuxeo-dm: same as nuxeo-cap but with Nuxeo DM
  * all-tests: run all tests

Here are some usage examples (ran from nuxeo-distribution):
  * (default) Building all Nuxeo products and their alternatives
    o mvn clean install
    o mvn clean install -Pall-distributions
  * Run all tests
    o mvn clean install -Pall-tests
  * Run Nuxeo DM functional tests
    o mvn clean install -Pftest-dm

Note: Maven expects multiple classifiers of an artifact to be deployed at the 
same time, else other classifiers previously built become unreachable from 
local and remote Maven repositories).
That means when you want to "deploy" (Maven remote deployment) or "install" (Maven
local deployment) a module, you must use "all-distributions" profile.

When you need only one classifier, for any other purpose than
install/deploy to m2 repository, then you can use the dedicated profiles.


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

### Nuxeo Digital Assets Management

Multimedia document collection management features.

Based on the addon <https://github.com/nuxeo/nuxeo-dam>.

Built NXR is in `nuxeo-distribution-dam/target/`.

It is installable in the default available application in `nuxeo-distribution-tomcat/target/` when running the wizard and selecting DAM (for users), or by activating the "nuxeo-dam" preset (for developers).

### Nuxeo Social Collaboration

Social network features (social workspaces, user relationships, mini-messages, user activity stream, ...).

Based on the addon <https://github.com/nuxeo/nuxeo-social-collaboration>.

Built NXR is in `nuxeo-distribution-social-collaboration/target/`.

It is installable in the default available application in `nuxeo-distribution-tomcat/target/` when running the wizard and selecting SC (for users), or by activating the "nuxeo-sc" preset (for developers).

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

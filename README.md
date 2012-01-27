# About the Nuxeo Distribution module

See <http://doc.nuxeo.org/> for full documentation.

This module builds, packages and tests the Nuxeo products.

## Requirements

Building Nuxeo products requires the following tools:
  * JDK 6 (Java Sun recommended)
  * Apache Maven 2.2.1
  * Apache Ant 1.7.1+
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

 * All JBoss packages:

        mvn clean package -Pjboss

 * All Tomcat packages:

        mvn clean package -Ptomcat

### Build with Ant

Ant is available at the top level: Ant targets have been defined to provide
user-friendly commands for most used build cases.

Ant usage: `ant distrib [-Ddistrib=profile]`

#### Examples

        ant distrib
        ant distrib -Ddistrib=nuxeo-dm
        ant distrib -Ddistrib=tomcat


### Available Maven profiles

 * nuxeo-coreserver, nuxeo-cap, nuxeo-dm, nuxeo-dam, nuxeo-cmf, ...: build the corresponding application/module
 * tomcat, jboss: build the corresponding server
 * all-distributions: full build
 * itest: run integration tests
 * ftest-dm, ftest-dam, ftest-cmf, ftest-sc, ...: run functional tests against the corresponding application/module
 * fltest-dm: run functional Funkload tests against DM
 * sdk: build SDK distributions for use in Nuxeo IDE

## Modules listing

 * nuxeo-distribution-cap: Content Application Platform EAR
 * nuxeo-distribution-cap-tests: WebDriver tests for CAP
 * nuxeo-distribution-cmf: Case Management EAR
 * nuxeo-distribution-coreserver: CoreServer EAR
 * nuxeo-distribution-dam: Digital Assets Management EAR
 * nuxeo-distribution-dm: Document Management EAR
 * nuxeo-distribution-jboss: JBoss distributions
 * nuxeo-distribution-resources: Resources archives used in other packagings (doc, binaries, templates).
 * nuxeo-distribution-social-collaboration: Social Collaboration EAR
 * nuxeo-distribution-tests: Helper POM with Nuxeo test dependencies
 * nuxeo-distribution-tomcat: Tomcat distributions
 * nuxeo-distribution-tomcat-tests: WebDriver tests for Tomcat
 * nuxeo-functional-tests: Framework for testing nuxeo distributions
 * nuxeo-launcher: Control Panel and launcher
 * nuxeo-marketplace-cmf: Marketplace package of CMF
 * nuxeo-marketplace-content-browser: Virtual marketplace package to manage compatibility between applications on top of CAP.
 * nuxeo-marketplace-dam: Marketplace package of DAM
 * nuxeo-marketplace-dm: Marketplace package of DM
 * nuxeo-marketplace-social-collaboration: Marketplace package of Social Collaboration
 * nuxeo-startup-wizard: Startup Wizard WebApp

## Produced packages

 * standalone EAR-like packages
     * Content Application Platform (CAP)
     * Advanced Document Management (DM)
     * Digital Assets Management (DAM)
     * Case Management (CMF)
     * Social Collaboration
 * Marketplace packages
 * Tomcat packages
 * JBoss packages


## Understanding Maven profiles and classifiers

Profiles are mainly used to manage the list of classifiers being generated.
Maven plugins rely by default on a such mechanism for creating tests, sources and
javadoc jars. It is usable also for any other specific builds (OS, JDK, env, packaging, ...).
It's widely used by a lot of third-parties (google gwt, json, shindings, ...).
Think about "classifiers" as "qualifiers" (sources, javadoc, tests, linux, windows,
mac, jta, all, ...). For example, the following are two alternatives ("classifiers")
for the package ("artifact") named "nuxeo-distribution-tomcat":
  * nuxeo-distribution-tomcat-5.4.0-SNAPSHOT-nuxeo-dm-jtajca.zip
  * nuxeo-distribution-tomcat-5.4.0-SNAPSHOT-nuxeo-dm.zip

Some profiles are used to choose the product to build. Other profiles are used to
choose which alternatives (classifiers) of the product will be built. Multiple
profiles can be used simultaneously.
Here are some common profiles and their impact on build result:
  * all-distributions: build everything except the Windows installer
  * all: build all classifiers for the called module(s)
  * nuxeo-cap: build only Nuxeo CAP classifier (if JBoss module, so build only
    JBoss with Nuxeo CAP)
  * nuxeo-dm: same as nuxeo-cap but with Nuxeo DM
  * shell: package a Nuxeo Shell
  * jboss: package a Nuxeo within JBoss

Here are some usage examples (ran from nuxeo-distribution):
  * (default) Building Nuxeo CAP, nuxeo DM with VCS, JBoss with Nuxeo DM on VCS
    o mvn clean install
    o mvn clean install -Pjboss,nuxeo-dm,vcs
  * Building all Nuxeo DM alternatives
    o mvn clean install -Pnuxeo-dm,all
  * Building all JBoss packagings
    o mvn clean install -Pjboss,all
  * Building all Nuxeo products and their alternatives
    o mvn clean install -Pall-distributions

Note: because of a Maven bug making things crazy when two classifiers of an
artifact are not deployed at the same time (i.e. if you deploy only nuxeo-cap
classifier of nuxeo-distribution-jboss, then nuxeo-dm classifier becomes
unreachable from local and remote Maven repositories).
That means when you want to "deploy" (Maven remote deployment) or "install" (Maven
local deployment) a module, you must use "all" profile.

When you need only one classifier, for any other purpose than
install/deploy to m2 repository, then you can use the dedicated profiles.


## Details about predefined applications

1. Nuxeo CAP

  Built EAR is in nuxeo-distribution-cap/target/ and its name depends on chosen
  package: default is nuxeo.ear


2. Nuxeo Document Management

  Built EAR is in nuxeo-distribution-dm/target/


4. Nuxeo Core Server

  A minimal server EAR. An embedded repository will be started. No other
  platform services are available.

  This application can be used to debug, test or develop nuxeo components that
  need a repository connection.

  Remoting will be also available in the future via Nuxeo Runtime.

  Built EAR is in nuxeo-distribution-coreserver/target/


7. Nuxeo Tomcat WebApp

  A Nuxeo Server packaged with Tomcat.
  This build will generate a zip containing a 'tomcat' directory.

  Nuxeo will be available at htpp://localhost:8080/nuxeo

  Built application is in nuxeo-distribution-tomcat/target/


## Custom build

It is of course possible to create custom builds.

For historical reasons, there are multiple technologies used for packaging in
nuxeo-distribution project (maven-assembly-plugin, maven-nuxeo-plugin,
maven-antrun-extended-plugin, nuxeo-distribution-tools).

They are all based on Maven principles with the objectives to avoid duplication,
ease maintenance and upgrade, rely on Maven artifacts, be OS independent.

We recommend to use our newest tool "nuxeo-distribution-tools".

Execution of the assembly may be done from Maven execution as a Maven plugin,
from command line or from Ant.

Based on Ant syntax, it provides access to major Maven concepts and Ant flexibility.

Principles of an assembly are generally to:
    * inherit a Maven dependency tree (list of artifacts to retrieve)
    * use this dependency tree to dispatch artifacts into directories
    * download complementary artifacts (default packaging, resources, drivers, ...)
    * download an empty server (JBoss, Jetty, Tomcat, ...)
    * assemble all those parts into a runnable product.

See the chosen tool documentation for more details.

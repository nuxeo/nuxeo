# Nuxeo Platform Signature


This is a digital signature plugin for signing PDF files. It provides multiple functionalities related to digital signing of documents, among others:

1. to create user certificates and store them within the Nuxeo CAP Instance.
2. to sign pdf documents
3. to share/download the local root certificate used for signing all documents within the domain


<A name="buildinganddeploying"></A>
## Building and deploying

To see the list of all commands available for building and deploying, use the following:

    $ ant usage

### How to build

You can build Nuxeo Digital Signature plugin with:

    $ ant build

If you want to build and launch the tests, do it with:

    $ ant build-with-tests

### How to deploy

Configure the build.properties files (starting from the `build.properties.sample` file to be found in the current folder), to point your Tomcat instance:

    $ cp build.properties.sample build.properties
    $ vi build.properties

You can then deploy Nuxeo Digital Signature to your Tomcat instance with:

    $ ant deploy-tomcat

You can also take all generated jar files (currently 3, present in the target directories of all submodules of this project), copy them into `$NUXEO_HOME/templates/custom/bundles/` and activate the "custom" template.


## Project Structure

This project can be divided conceptually into 3 parts:

1) certificate generation (low-level PKI object operations, CA operations)

2) certificate persistence (storing and retrieving keystores containing certificates inside nuxeo directories)

3) pdf signing with an existing certificate


## Configuration:

1) Install your root keystore file in a secured directory

To do initial testing you can use the keystore specified in:
./nuxeo-platform-signature-core/src/main/resources/OSGI-INF/root-contrib.xml

2) You might have to modify your server system's java encryption configuration by installing JCE Unlimited Strength Jurisdiction Policy Files needed for passwords longer than 7 characters,

*Note: cryptography exportation laws differ between countries so make sure you are using adequate encryption configuration, libraries and tools.*


## QA results

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_nuxeo-signature-master)](https://qa.nuxeo.org/jenkins/job/addons_nuxeo-signature-master/)

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
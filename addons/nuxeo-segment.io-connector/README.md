nuxeo-segment.io-connector
==========================

Integrate segment.io API with Nuxeo Event system

## Principles

The bundles provides a service that wraps the Analytics Java lib provided by segment.io

    Framework.getLocalService(SegmentIO.class);

This Service gives access to the 2 main API entry points : `identify`and `track`.

Although this service can be used directly, the typical use case is to use the provided infrastructure to bridge Nuxeo events to segment.io calls.

For this, a generic listener is provided : based on the configuration it will convert / map Nuxeo events to calls to segment.io API.

A typical use case could be :

    `loginSucceed`    in Nuxeo => call identify on segment.io
    `documentCreated` in Nuxeo => call track    on segment.io

## Building / Install

Build :

    mvn clean install

Install bundle

    cp target/nuxeo-segmentio-connector-5.8.jar $NUXEO_HOME/templates/custom/bundles/

Install lib

    cp ~/.m2/repository/com/github/segmentio/analytics/0.3.1/analytics-0.3.1.jar $NUXEO_HOME/templates/custom/lib/
    cp ~/.m2/repository/com/google/code/gson/gson/2.2/gson-2.2.jar $NUXEO_HOME/templates/custom/lib/

## Downloadling

The project is automatically built by http://qa.nuxeo.org/jenkins/job/addons_nuxeo-segment.io-connector-master/

You can download the last stable JAR from: http://qa.nuxeo.org/jenkins/job/addons_nuxeo-segment.io-connector-master/lastSuccessfulBuild/artifact/target/

## Server side integration

The SegmentIO API is exposed a Nuxeo Service and plugged to Nuxeo event bus via a mapping system.

The mapping is managed via the `mapping` extenson point.

The configuration mapping is based on Groovy Scripting engine.

### Default mapping and Groovy context

When calling the segment.io API (identify or track), Nuxeo service will always send a minimal set of metadata :

 - `email` :  `principal.getEmail()`
 - `firstName` : `principal.getFirstName()`
 - `lastName` : `principal.getLastName()`

However, you can override this default binding, by providing custom bindng for these fields `email`, `firstName`, `lastName`.

The parameters binding, is generated from a Groovy script evaluated in a context containing :

 - `event` : the Nuxeo event object
 - `eventContext` : context of the event (shortcut for `event.getContext()`)
 - `principal` : the `NuxeoPrincipal` user object
 - `mapping` : the HashMap that will be used to resolve the mapping

If the event is associated to a Document event, the context will also contain :

 - `doc` : the source DocumentModel for the event
 - `repository` : name of the source repository
 - `session` : CoreSession
 - `dest` : DocumentRef to the target Document if any

The `principal` is normally extracted from the context and will be used to fetch the default values for `email`, `firstName`, `lastName`. If you need to use a different value for the principal, you can simply override the default resolution by putting a `principal` field in the binding.

### Parameters bindings

Here is a simple binding :

*Bind `loginSucceed` to call identify on segment.io*

    <mapper name="testIdentify" targetAPI="Identify">
      <events>
         <event>loginSuccess</event>
      </events>
      <parameters>
        <parameter name="plugin">eventContext.getProperty("AuthenticationPlugin")</parameter>
        <parameter name="company">principal.getCompany()</parameter>
      </parameters>
    </mapper>

*Bind `documentCreated` to call track on segment.io and propagate document title*

    <mapper name="testTrac" >
      <events>
         <event>documentCreated</event>
      </events>
      <parameters>
        <parameter name="title">doc.getTitle()</parameter>
      </parameters>
    </mapper>

### Direct Groovy scripting

The `parameters` system is used to generate a Groovy script.

Typically, this XML configuration

      <parameters>
        <parameter name="plugin">eventContext.getProperty("AuthenticationPlugin")</parameter>
        <parameter name="company">principal.getCompany()</parameter>
      </parameters>

Will result in this script

      <groovy>
        mapping.put("plugin", );
        mapping.put("company", principal.getCompany());
      </groovy>

If needed you can directly contribute the groovy script in addition or in replacement of the parameters binding.

### About Grouping

The SegmentIO java lib does not expose a group API, but the corresponding REST API does exist.

So, the Nuxeo integration module will try to automatically generate a `group` call uppon `identify` is the mapping contains some meta-data about the groups.

Technically, all meta-data starting with `group_` will be considered as group meta-data, so just adding a `group_id` and `group_name` to the mapping should generate a call to the `group` REST API.

## Client side integration

It may be useful to also provide a client side integration.

### JSF / BackOffice

When deployed the segment-io bundle will automatically extend the default footer to include segment.io script.

This includes :

 - dynamic loading analytics.js + dependencies
 - init analytics.js with the configured WriteKey
 - calling `page` API
 - calling `identify` API once per session

You can also do a direct integration in your xhtml template using :

    <ui:include src="/incl/segmentio.xhtml" />

### Other Web UI

Outside of JSF, for example in WebEngine, you can include a dynamically generated script :

    <script src="/nuxeo/site/segmentIO"></script>

This script will :

 - dynamic loading analytics.js + dependencies
 - init analytics.js with the configured WriteKey
 - calling `page` API

If you want to call the `identify` API, you can directly call

    identifyIfNeeded(login, email)

A typical call in the context of a WebEngine page would be :

    identifyIfNeeded('${Context.principal.name}', '${Context.principal.email}');

NB : this script is served as anonymous.

An alternative is to use a script that is generated for your user:

    <script src="/nuxeo/site/segmentIO/user/XXX"></script>

XXX should be the actual login of the currently loggedin user.

Typically :

    <script src="/nuxeo/site/segmentIO/user/${Context.principal.name}"></script>

NB : this script is served for authenticated users.


## QA results

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_nuxeo-segment.io-connector-master)](https://qa.nuxeo.org/jenkins/job/addons_nuxeo-segment.io-connector-master/)

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Netflix, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.

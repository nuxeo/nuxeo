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

    cp target/nuxeo-segmentio-connector-5.8.jar  $NX_TOMCAT/nxserver/plugins/.

Install lib

    cp ~/.m2/repository/com/github/segmentio/analytics/0.3.1/analytics-0.3.1.jar  $NX_TOMCAT/nxserver/lib/.

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

An alternative is to use a script that is generated for your user:

    <script src="/nuxeo/site/segmentIO?login=XXX"></script>

XXX should be the actual login of the currently loggedin user.

Typically : 

    <script src="/nuxeo/site/segmentIO?login=${Context.principal.name}"></script>





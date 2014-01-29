nuxeo-segment.io-connector
==========================

Integrate segment.io API with Nuxeo Event system

### Principles

The bundles provides a service that wraps the Analytics Java lib provided by segment.io

    Framework.getLocalService(SegmentIO.class);

This Service gives access to the 2 main API entry points : `identify`and `track`.

Although this service can be used directly, the typical use case is to use the provided infrastructure to bridge Nuxeo events to segment.io calls.

For this, a generic listener is provided : based on the configuration it will convert / map Nuxeo events to calls to segment.io API.

A typical use case could be :

    `loginSucceed`    in Nuxeo => call identify on segment.io
    `documentCreated` in Nuxeo => call track    on segment.io	

### Building / Install

Build : 

    mvn clean install

Install bundle

    cp target/nuxeo-segmentio-connector-5.8.jar  $NX_TOMCAT/nxserver/plugins/.

Install lib

    cp ~/.m2/repository/com/github/segmentio/analytics/0.3.1/analytics-0.3.1.jar  $NX_TOMCAT/nxserver/lib/.

### About mapping

The mapping is managed via the `mapping` extenson point.

The configuration mapping is based on Groovy Scripting engine.

#### Default mapping and Groovy context

When calling the segment.io API (identify or track), Nuxeo service will always send a minimal set of metadata :

 - `email` :  `principal.getEmail()`
 - `firstName` : `principal.getFirstName()`
 - `lastName` : `principal.getLastName()`
 - `email` : `principal.getEmail()`

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

#### Parameters bindings

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

#### Direct Groovy scripting

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


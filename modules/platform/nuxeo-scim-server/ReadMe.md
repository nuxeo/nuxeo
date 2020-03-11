nuxeo-scim-server
=================

# About

This Nuxeo plugin provides an implementation of the [SCIM](http://www.simplecloud.info/) 1.1 API on top of Nuxeo `UserManager`.

The goal is to allow a third party IDM (ex: Okta, OneLogin, PingIdentity ...) to provision Users and Groups directly inside Nuxeo Platform based applications.

# Implementation

## Core Objects and Marshaling

The implemenation is based on [SCIMSDK](https://code.google.com/p/scimsdk/) but the JAX-RS part relies on WebEngine/JAX-RS stack that is integrated inside Nuxeo Platform rather than on Apache Wink.

## Schemas

The Schemas (XSD and JSON) are taked from [SCIM](http://scim.googlecode.com/svn/trunk) project that contains the spec and the associated docs.

## Tests

Most of the testing part is based on the [SCIMProxy](http://scimproxy.googlecode.com/svn) project that contains Object model + Client + Compliancy Test suite.

NB : Because all the tests are dependent in the `ScimCore` artifact provided by `ScimProxy`, it may make sense to switch all Nuxeo implementation to this lib and drop the dependency on `ScimSDK`.

# Building

    mvn clean install

# Status

## Quick Status

 - CRUD API is implemented
 - PATCH is not supported
 - Bulk mode is not supported
 - User/Groups attributes is not configurable

## Compliancy tests

     Success: Read ServiceProviderConfig
     Success: Read schema for Users
     Success: Read schema for Groups
     Success: Create user in json
     Success: Create user in json
     Success: Create user in json
     Success: Create group in json
     Success: Create user in xml
     Success: Create user in xml
     Success: Create user in xml
     Success: Create group in xml
     Success: PUT User JSON
     Success: PUT User XML
     Success: PUT Group JSON
     Success: PUT Group XML
     Success: Delete user
     Success: Delete non-existing user
     Success: Delete user
     Success: Delete non-existing user
     Success: Delete user
     Success: Delete non-existing user
     Success: Delete user
     Success: Delete non-existing user
     Success: Delete user
     Success: Delete non-existing user
     Success: Delete user
     Success: Delete non-existing user
     Success: Delete group
     Success: Delete non-existing group
     Success: Delete group
     Success: Delete non-existing group
     Ran 31 compliancy tests :
        31 success
         0 skipped
         0 failed

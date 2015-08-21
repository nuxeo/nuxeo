nuxeo-usermapper
==========================


## Principles

### Use cases

We currently have several places where we need to Create/Update a Nuxeo User (and possibly groups) from data provided by an external system.

This can typically be :

 - an Authentication plugin that handles Just In Time user provisioning
     ** Shibboleth
     ** SAML
 - a provisioning API like [SCIM|http://www.simplecloud.info/]
 
This means that we currently have code that handle this at several places :

 - [nuxeo-platform-login-shibboleth](https://github.com/nuxeo/nuxeo-platform-login/tree/master/nuxeo-platform-login-shibboleth)
 - [saml](https://github.com/nuxeo/nuxeo-platform-login/tree/feature-NXP-14596-Okta-integration/nuxeo-platform-login-okta)
 - [nuxeo-scim-server](https://github.com/tiry/nuxeo-scim-server)
 
Having this code duplicated is clearly not good from a maintenance point of view, but in addition it means we have different level of services regarding user's attributes and groups.

So, it may be worth having global User/Group mapping service.

###. UserMapper Service

#### Configurable mapping

Of course, we need the mapping to be configurable, but unfortunately, the source object is different depending on the source : SAML user, Shibboleth user, SCIM user.

Ideally, we would like to rely on a key value system (i.e. see user and group as a Map) with simple mapping, but :

 - SCIM Model is more complex than simple Key/Value
 - some time we need to compute some attributes (like : FullName = FirstName + LastName)

A simple option would be to have a mapping that is configured using a Groovy scriptlet, like it is done for [nuxeo-segment-io-connector](https://github.com/tiry/nuxeo-segment.io-connector).

#### 2 Ways mapping

At least for SCIM use cases, the Service needs to handle 2 ways :


     getCreateOrUpdateNuxeoPrinciple(String mappingName, Object user) 

This API will be used to create / update a Nuxeo Principal based on SCIM user object.

     Object wrapNuxeoPrincipal(String mappingName, NuxeoPrincipal principal)

Get the SCIM representation of a Nuxeo User.

## Building / Install

Build : 

    mvn clean install




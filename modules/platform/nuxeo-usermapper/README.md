nuxeo-usermapper
==========================

## Principles

### Use cases

We currently have several places where we need to Create/Update a Nuxeo User (and possibly groups) from data provided by an external system.

This can typically be :

 - an Authentication plugin that handles Just In Time user provisioning
     - Shibboleth
     - SAML
     - OpenId
     - Jboss Keycloak
 - a provisioning API like [SCIM](http://www.simplecloud.info/)

The goal of this module is double :

 - avoid duplicated code in several modules
 - make the mapping pluggable

### UserMapper Service

#### Configurable mapping

Of course, we need the mapping to be configurable, but unfortunately, the source object is different depending on the source : SAML user, Shibboleth user, SCIM user.

Ideally, we would like to rely on a key value system (i.e. see user and group as a Map) with simple mapping, but :

 - SCIM Model is more complex than simple Key/Value
 - some time we need to compute some attributes (like : FullName = FirstName + LastName)

For this reason, the mapping can be contributed :

 - as a Java Class
 - as Groovy Scriptlets
 - as JavaScript

#### 2 Ways mapping

At least for SCIM use cases, the Service needs to handle 2 ways :

     NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(Object userObject, boolean createIfNeeded, boolean update,
            Map<String, Serializable> params);

This API will be used to create / update a Nuxeo Principal based on SCIM user object.

     Object wrapNuxeoPrincipal(NuxeoPrincipal principal, Object nativePrincipal, Map<String, Serializable> params);

Get the SCIM representation of a Nuxeo User.

#### Contributing new mapping

The component expose a `mapper` extension point that can be used to contribute new mappers.

Using plain Java Code :

    <mapper name="javaDummy" class="org.nuxeo.usermapper.test.dummy.DummyUserMapper">
       <parameters>
         <param name="param1">value1</param>
       </parameters>
    </mapper>

Using Groovy Scriptlet :

    <mapper name="scim" type="groovy">
      <mapperScript>
      <![CDATA[
          import org.nuxeo.ecm.platform.usermanager.UserManager;
          import org.nuxeo.runtime.api.Framework;

          UserManager um = Framework.getLocalService(UserManager.class);

          String userId = userObject.getId();
          if (userId == null || userId.isEmpty()) {
            userId = userObject.getUserName();
          }
          ...
        ]]>
      </mapperScript>

      <wrapperScript>
        <![CDATA[
          import org.nuxeo.ecm.core.api.DocumentModel;
          import org.nuxeo.ecm.core.api.NuxeoException;
          import org.nuxeo.ecm.platform.usermanager.UserManager;
          import org.nuxeo.runtime.api.Framework;

          UserManager um = Framework.getLocalService(UserManager.class);
          DocumentModel userModel = nuxeoPrincipal.getModel();
          ...
        ]]>
      </wrapperScript>
    </mapper>

Using JavaScript :

    <mapper name="jsDummy" type="js">
      <mapperScript>
          searchAttributes.put("username", userObject.login);
          userAttributes.put("firstName", userObject.name.firstName);
          userAttributes.put("lastName", userObject.name.lastName);
          profileAttributes.put("userprofile:phonenumber", "555.666.7777");
       </mapperScript>
     </mapper>

**mapperScript**

In the script context for mapping userObject to NuxeoPrincipal (i.e. `mapperScript` tag corresponding to the `getOrCreateAndUpdateNuxeoPrincipal`)

 - userObject : represent the object passed to the
 - searchAttributes : is the Map&lt;String, String&gt; that will be used to search the NuxeoPrincipal
 - userAttributes : is the Map&lt;String, String&gt; that will be used to create/update the NuxeoPrincipal
 - profileAttribute : is the Map&lt;String, String&gt; that will be used to update the user's profile

**wrapperScript**

In the script context for wrapping a NuxeoPrincipal into a userObject (i.e. `wrapperScript` tag corresponding to the `wrapNuxeoPrincipal` method) :

 - userObject : represent the userObject as initialized by the caller code
 - nuxeoPrincipal : is the principal to wrap
 - params : is the Map&lt;String, Serializable&gt; passed by the caller

## Building / Install

Build :

    mvn clean install

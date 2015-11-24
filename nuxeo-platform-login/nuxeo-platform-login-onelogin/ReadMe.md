# nuxeo-platform-login-onelogin

## About

This nuxeo plugin integrate OneLogin SSO system inside Nuxeo Platform.

This allows to use OneLogin IDP for managing users and authentication.

## Building


   mvn clean install

## Open questions

 - Should I use SAML or the delegated Authentication API ?
     - I started using SAML
 - I may have missed something, but the java sample code seems to have issues
     - ex: sample code raises an exception (ex: `Response.tagIdAttributes`)
 - Users / Groups push from OneLogin to Nuxeo
     - should Nuxeo poll for events using the REST API ?



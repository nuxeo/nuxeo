# nuxeo-platform-login-okta

## About

This nuxeo plugin integrate Okta SSO system inside Nuxeo Platform.

This allows to use Okta IDP for managing users and authentication.

## Building


   mvn clean install

## Open questions

 - Can not access Nuxeo directly 
      - user is always being redirected to the Okta application page (even when he is already authenticated)
      - clicking on the Nuxeo App does sent the user to Nuxeo with the right credentials
 - Custom Attributes
      - did not find a way to define custom attributes that can be propagated to Nuxeo
 - Provisioning API
      - did not find a way to have Okta push user/group create/update/delete to Nuxeo
        (APIs probably exists, but does not seem to be public)



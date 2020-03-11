# Nuxeo Platform Login Keycloak

This repo hosts the source code of a Keycloak plugin for Nuxeo Platform.

About Keycloak: Keycloak is an brand new integrated SSO and IDM for browser apps and RESTful web services.
Built on top of the OAuth 2.0, Open ID Connect, JSON Web Token (JWT) and SAML 2.0 specifications

## Building and deploying

- Install a Nuxeo server, version 6.x or higher.

- Download [keycloak tomcat 7 adpaters](http://sourceforge.net/projects/keycloak/files/1.3.1.Final/adapters)
  and unpack everything in your ${NUXEO_CONF}/templates/keycloak/nxserver/plugins directory (see "Sample" directory of this repository)

- Install maven 3+ and build _nuxeo-platform-login-keycloak_ by running:

        mvn clean install

- Copy _nuxeo-platform-login-keycloak_ your ${NUXEO_CONF}/templates/keycloak/nxserver/plugins directory by running:

        cp target/nuxeo-platform-login-keycloak-*.jar ${NUXEO_CONF}/templates/keycloak/nxserver/nxserver/plugins/

- Start Nuxeo and have a try!

## Goal

The main goal of this module is to allow a user or a client API that is registered in keycloak to acces nuxeo without log-in.
This plugin does multiple checking and operations to achieve this goal:

- Check authentication:
    - Either user's authentication using AOuth2 protocol
    - Or either a client api's authentication using HTTP Request header "Authorization: Bearer _token_"
- Retrieve current keycloak user using [keycloak's provided tomcat adapter](https://docs.jboss.org/keycloak/docs/1.2.0.CR1/userguide/html/ch08.html#tomcat-adapter)
- Map user's roles defined in keycloak to nuxeo roles

A user that has signed in keycloak then cas browser nuxeo's client application or use nuxeo Rest Api in another client.

### Authentication plugin

The module contributes a new ``authenticationPlugin`` called ``KEYCLOAK_AUTH``, that handles authentication with a token sent as a request header.
It uses the ``KeycloakAuthenticationPlugin`` to search get current user logged in keycloak.

This authentication plugin is configured to be used with the ``Trusting_LM`` ``LoginModule`` plugin =>
no password check is done, a principal will be created from the user name retrieved from keycloak and the user is created in user directory if it does not exist.

The automation-specific authentication chain is overridden to use the ``KEYCLOAK_AUTH`` plugin first.
Two specific authentication chains are also mapped to this Keycloak plugin.

    <extension
      target="org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService"
      point="specificChains">

      <specificAuthenticationChain name="Automation">
        <urlPatterns>
          <url>(.*)/automation.*</url>
        </urlPatterns>
        <replacementChain>
          <plugin>KEYCLOAK_AUTH</plugin>
          <plugin>AUTOMATION_BASIC_AUTH</plugin>
        </replacementChain>
      </specificAuthenticationChain>

      <specificAuthenticationChain name="RestAPI">
        <urlPatterns>
          <url>(.*)/api/v.*</url>
        </urlPatterns>

        <replacementChain>
          <plugin>KEYCLOAK_AUTH</plugin>
          <plugin>AUTOMATION_BASIC_AUTH</plugin>
        </replacementChain>
      </specificAuthenticationChain>

    </extension>

You can change these behaviours in the ``keycloak-descriptor-bundle.xml`` file to adapt this plugin to fit your needs

## About the contibutor

François Maturel, contributor of this plugin is a [senior JAVA / HTML / AngularJS
developper] (http://dijit.fr) based in Rennes, France.
Hope you'll enjoy keycloak SSO with Keycloak!

## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software
platform for enterprise content management] [5] and packaged applications
for [document management] [6], [digital asset management] [7] and
[case management] [8]. Designed by developers for developers, the Nuxeo
platform offers a modern architecture, a powerful plug-in model and
extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>

[1]: https://jira.nuxeo.com/browse/NXP-10268
[2]: https://github.com/nuxeo/nuxeo-drive
[3]: https://jira.nuxeo.com/browse/NXP-10269
[5]: http://www.nuxeo.com/en/products/ep
[6]: http://www.nuxeo.com/en/products/document-management
[7]: http://www.nuxeo.com/en/products/dam
[8]: http://www.nuxeo.com/en/products/case-management

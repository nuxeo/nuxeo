# Nuxeo Platform Login Keycloak

This repo hosts the source code of a Keycloak plugin for Nuxeo Platform.

About Keycloak: Keycloak is an brand new integrated SSO and IDM for browser apps and RESTful web services.
Built on top of the OAuth 2.0, Open ID Connect, JSON Web Token (JWT) and SAML 2.0 specifications

## Goal

The main goal of this module is to allow a user or a client API that is registered in keycloak to acces nuxeo without log-in.
This plugin does multiple checking and operations to achieve this goal:

- Check authentication:
    - Either user's authentication using AOuth2 protocol
    - Or either a client api's authentication using HTTP Request header "Authorization: Bearer _token_"
- Retrieve current keycloak user using [keycloak's provided tomcat adapter](https://www.keycloak.org/docs/latest/securing_apps/#_tomcat_adapter)
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

The [Nuxeo Platform](http://www.nuxeo.com/products/content-management-platform/) is an open source customizable and extensible content management platform for building business applications. It provides the foundation for developing [document management](http://www.nuxeo.com/solutions/document-management/), [digital asset management](http://www.nuxeo.com/solutions/digital-asset-management/), [case management application](http://www.nuxeo.com/solutions/case-management/) and [knowledge management](http://www.nuxeo.com/solutions/advanced-knowledge-base/). You can easily add features using ready-to-use addons or by extending the platform using its extension point system.

The Nuxeo Platform is developed and supported by Nuxeo, with contributions from the community.

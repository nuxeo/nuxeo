# Nuxeo Platform Login Token

This repo hosts the source code of a plugin for Nuxeo Platform that allows a token-based authentication.

The associated JIRA issue is: [NXP-10268] [1]

## Building and deploying

- Install a Nuxeo server, version 5.6 or higher.

- Install maven 2.2.1+ and build _nuxeo-platform-login-token_ by running:

        mvn clean install

- Deploy _nuxeo-platform-login-token_ in the Nuxeo server by running:

        cp target/nuxeo-platform-login-token-5.7-SNAPSHOT.jar $NUXEO_HOME/nxserver/plugins/

- Start Nuxeo and have a try!

## Goal

The main goal of this module is to allow a client device to authenticate against a Nuxeo server using a token acquired during a handshake phase and then stored locally for a regular use.
This way, the client does not need to store any user secret information such as login / password that could be found easily on a file system for example. If a specific device token is compromised (e.g. laptop theft), the user can revoke the device token in the web interface and generate a new one independent token for new device with their usual user credentials.

A token is bound on the server to a triplet defined by:

- a user name
- an application name
- a device identifier

This way a single user can have multiple tokens for different applications on different devices.

For example: the user _joe_ could have 3 tokens:

- one for the Nuxeo Drive client on his Linux box
- one for the Nuxeo Drive client on his Windows box
- one for a Nuxeo Automation client application on his Linux box

The module includes a UI for the user to manage their tokens.
For now a token can only be revoked, but for later we are planning of setting an expiration date on tokens with a possibility to renew them.

## Implementation

### Handshake phase

The ``TokenAuthenticationServlet``, protected by basic authentication and mapped on the ``/authentication/token`` URL pattern, allows to get a unique token given some user information passed as request parameters:
``applicationName``, ``deviceId``, ``deviceDescription`` and ``permission``.

The token is sent as plain text in the response body.

An error response will be sent with a 400 status code if one of the required parameters is null or empty.
All parameters are required except for the device description. The parameters are URI decoded by the Servlet.

For example, you could execute the following command to acquire a token:

    curl -H 'Authorization:Basic **********************' -G 'http://server:port/nuxeo/authentication/token?applicationName=Nuxeo%20Drive&deviceId=device-1&deviceDescription=My%20Linux%20box&permission=rw'

While the device description can typically be edited by the user (for instance in the JSF UI), both the Application Name and the device identifier should not change once the token has been generated.

### Token bindings

The ``TokenAuthenticationService`` handles the token generation and storage of the token bindings.

- A token is randomly generated using the ``UUID`` class from the JDK which ensures that it is unique and secure.
- Token bindings are persisted in a SQL directory, using the token as a primary key.

Looking back at the example of _joe_ and his 3 tokens, the server would hold these 3 token bindings:

- {'tokenA', 'joe', 'Nuxeo Drive', 'device-1'}
- {'tokenB', 'joe', 'Nuxeo Drive', 'device-2'}
- {'tokenC', 'joe', 'Automation client', 'device-1'}

### Authentication plugin

The module contributes a new ``authenticationPlugin`` called ``TOKEN_AUTH``, that handles authentication with a token sent as a request header.
It uses the ``TokenAuthenticationService`` to search for a user name bound to the given token.

This authentication plugin is configured to be used with the ``Trusting_LM`` ``LoginModule`` plugin =>
no password check is done, a principal will be created from the user name if the user exists in the user directory.

The token must be put in a request header called ``X-Authentication-Token``.

The automation-specific authentication chain is overridden to use the ``TOKEN_AUTH`` plugin just after the basic authentication one.
A specific authentication chain is also mapped on the token request header.

    <extension
      target="org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService"
      point="specificChains">

      <specificAuthenticationChain name="Automation">
        <urlPatterns>
          <url>(.*)/automation.*</url>
        </urlPatterns>
        <replacementChain>
          <plugin>AUTOMATION_BASIC_AUTH</plugin>
          <plugin>TOKEN_AUTH</plugin>
          <plugin>ANONYMOUS_AUTH</plugin>
        </replacementChain>
      </specificAuthenticationChain>

      <specificAuthenticationChain name="TokenAuth">
        <headers>
          <header name="X-Authentication-Token">.*</header>
        </headers>
        <replacementChain>
          <plugin>TOKEN_AUTH</plugin>
        </replacementChain>
      </specificAuthenticationChain>

    </extension>

### UI

The module provides the ``auth_token_bindings.xhtml`` view that includes the ``authTokenBindings`` layout to display the list of token bindings for the current user, with a _Revoke_ action on each token.

For now, as this module is mostly dedicated to [Nuxeo Drive] [2] (also see [NXP-10269] [3]), it only provides a layout and XHTML view for listing token bindings,
but does not include this view by default in the User Center. It will be used in the specific _Nuxeo Drive_ tab of the User Center.


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

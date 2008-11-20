Setting Up Anonymous Authentication
-----------------------------------

This description is for a mono-machine configuration.

To enable anonymous authentication, the plugin
nuxeo-platform-login-anonymous-XYZ.jar must be placed in your
server/default/deploy/nuxeo.ear/plugins directory, and the configuration file
login-anonymous-config.xml should be placed in the
server/default/deploy/nuxeo.ear/config directory.

You should edit the config file to specify the desired login name for the
Anonymous user, and its characteristics (first and last name for instance).


Setting Up Anonymous Authentication for a multi-machine configuration
---------------------------------------------------------------------

This description is for a multi-machine configuration, specifically for a
stateful + stateless configuration.

The plugin nuxeo-platform-login-anonymous-XYZ.jar must be placed in the
server/default/deploy/nuxeo.ear/plugins directory of the stateless instance(s).

Then there are dedicated configuration files for the stateful instance and the
stateless instance(s).
The configuration file login-anonymous-stateful-config.xml should be placed in
the server/default/deploy/nuxeo.ear/config of the stateful instance while the
the configuration file login-anonymous-stateless-config.xml should be placed in
the server/default/deploy/nuxeo.ear/config of the stateless instance(s).

The login-anonymous-stateful-config.xml file could be modified to specify the
desired login name for the Anonymous user, and its characteristics (first and
last name for instance).



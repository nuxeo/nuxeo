This jar contains extension functions for H2 and Derby (and maybe
in the future other databases) to implement some stored procedures
needed for Nuxeo.

It needs to be be deployed in the application server in a classloader
accessible by the JDBC connectors.

In JBoss, this means putting it in $JBOSS/server/default/lib
for instance.

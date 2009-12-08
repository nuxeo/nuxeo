This module implements a component through which datasources can be registered
and bound to an internal JNDI implementation.

Example contribution for a non-XA datasource:

  <extension target="org.nuxeo.runtime.datasource" point="datasources">
    <datasource name="jdbc/foo" driverClassName="org.apache.derby.jdbc.EmbeddedDriver"
        maxActive="20" maxIdle="5" maxWait="10000">
      <property name="url">jdbc:derby:/home/db;create=true</property>
      <property name="username">nuxeo</property>
      <property name="password">nuxeo</property>
    </datasource>
  </extension>

Example contribution for a XA datasource (see the documentation for the
setters of the chosen JDBC XA datasource for the exact properties to use):

  <extension target="org.nuxeo.runtime.datasource" point="datasources">
    <datasource name="jdbc/foo" xaDataSource="org.apache.derby.jdbc.EmbeddedXADataSource"
        maxActive="20" maxIdle="5" maxWait="10000">
      <property name="databaseName">/home/db</property>
      <property name="createDatabase">create</property>
      <property name="user">nuxeo</property>
      <property name="password">nuxeo</property>
    </datasource>
  </extension>

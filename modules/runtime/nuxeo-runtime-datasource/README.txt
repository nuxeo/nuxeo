This module implements a component through which datasources can be registered
and bound to an internal JNDI implementation.

Example contribution for a non-XA datasource:

  <extension target="org.nuxeo.runtime.datasource" point="datasources">
    <datasource name="jdbc/foo" driverClassName="org.h2.Driver"
        maxActive="20" maxIdle="5" maxWait="10000">
      <property name="url">jdbc:h2:/home/db;DB_CLOSE_ON_EXIT=false</property>
      <property name="username">nuxeo</property>
      <property name="password">nuxeo</property>
    </datasource>
  </extension>

Example contribution for a XA datasource (see the documentation for the
setters of the chosen JDBC XA datasource for the exact properties to use):

  <extension target="org.nuxeo.runtime.datasource" point="datasources">
    <datasource name="jdbc/foo" xaDataSource="org.h2.jdbcx.JdbcDataSource"
        maxActive="20" maxIdle="5" maxWait="10000">
      <property name="databaseName">/home/db</property>
      <property name="createDatabase">create</property>
      <property name="user">nuxeo</property>
      <property name="password">nuxeo</property>
    </datasource>
  </extension>

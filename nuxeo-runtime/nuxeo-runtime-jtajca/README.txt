This module registers a component that defines a Transaction Manager as well
as a JCA-compatible Connection Manager in order for JCA resources to be
usable with connection pooling.

The current implementation is based on Apache Geronimo's geronimo-transaction
and geronimo-connector.

To bind the transaction manager and Nuxeo connection manager in JNDI,
under Tomcat you can use:

  <Resource name="TransactionManager" auth="Container"
      type="javax.transaction.TransactionManager"
      factory="org.nuxeo.runtime.jtajca.NuxeoTransactionManagerFactory"
      transactionTimeoutSeconds="300"/>

  <Transaction 
      factory="org.nuxeo.runtime.jtajca.NuxeoUserTransactionFactory"/>

  <Resource name="NuxeoConnectionManager" auth="Container"
      type="javax.resource.spi.ConnectionManager"
      factory="org.nuxeo.runtime.jtajca.NuxeoConnectionManagerFactory"
      minPoolSize="0" maxPoolSize="20" idleTimeoutMinutes="0"/>

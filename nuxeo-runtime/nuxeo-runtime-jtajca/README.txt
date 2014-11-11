This module registers a component that defines a Transaction Manager as well as
a JCA-compatible Connection Manager in order for JCA resources to be usable
with connection pooling.

The current implementation is based on Apache Geronimo's geronimo-transaction and geronimo-connector.

The Transaction Manager is bound on java:comp/TransactionManager
The User Transaction is bound on java:comp/UserTransaction
The Connection Manager is bound on java:comp/NuxeoConnectionManager

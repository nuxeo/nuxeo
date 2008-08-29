Nuxeo repository SQL connector installation documentation

1. Install nuxeo

Download the latest 5.1.6 snapshot, for instance
http://www.nuxeo.org/static/snapshots/nuxeo-ep-5.1.6.SNAPSHOT-installer-20080810.jar

You can also use Nuxeo 5.1.5 provided you install specially backported Jars in it,
see http://jira.nuxeo.org/browse/NXP-2567.

Install Nuxeo in a directory called $JBOSS in this document.

2. Remove the old JCR connector

rm $JBOSS/server/default/deploy/nuxeo.ear/config/default-repository-config.xml
rm $JBOSS/server/default/deploy/nuxeo.ear/config/default-versioning-config.xml
rm $JBOSS/server/default/deploy/nuxeo.ear/system/nuxeo-core-jcr-connector-1.4.3-SNAPSHOT.jar
rm $JBOSS/server/default/deploy/nuxeo.ear/system/nuxeo-core-jca-1.4.3-SNAPSHOT.rar
  
3. Install the SQL connector

cp nuxeo-core-storage-sql-ra-1.4.3-SNAPSHOT.rar $JBOSS/server/default/deploy/nuxeo.ear/system
cp nuxeo-core-storage-sql-1.4.3-SNAPSHOT.jar $JBOSS/server/default/deploy/nuxeo.ear/system

4. Configure the SQL connector

Copy to $JBOSS/server/default/deploy/nuxeo.ear/config/repository-sql-config.xml

<?xml version="1.0"?>
<component name="org.nuxeo.ecm.core.storage.sql.config">
  <extension target="org.nuxeo.ecm.core.repository.RepositoryService"
    point="repository">
    <repository name="default"
      factory="org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryFactory">
      <repository name="default">
      </repository>
    </repository>
  </extension>
  <extension target="org.nuxeo.ecm.core.lifecycle.LifeCycleService"
    point="lifecyclemanager">
    <lifecyclemanager
      class="org.nuxeo.ecm.core.storage.sql.coremodel.SQLLifeCycleManager" />
  </extension>
</component>

(This extension point will in the future be made extensible to specify various
configuration options, for new the binaries storage is hardcoded to
$JBOSS/server/default/data/NXRuntime/binaries)

Install the appropriate JDBC driver in $JBOSS/server/default/lib, for instance
derby-10.4.1.3.jar, or postgresql-8.2-507.jdbc3.jar.

Create the datasource file
$JBOSS/server/default/deploy/nuxeo.ear/datasources/repository-sql-ds.xml
For Derby, use the following datasource:

<?xml version="1.0"?>
<connection-factories>
  <tx-connection-factory>
    <jndi-name>NXRepository/default</jndi-name>
    <adapter-display-name>Nuxeo SQL Repository DataSource</adapter-display-name>
    <rar-name>nuxeo.ear#nuxeo-core-storage-sql-ra-1.4.3-SNAPSHOT.rar</rar-name>
    <connection-definition>org.nuxeo.ecm.core.storage.sql.Repository</connection-definition>
    <xa-transaction/>
    <config-property name="name">default</config-property>
    <config-property name="xaDataSource" type="java.lang.String">org.apache.derby.jdbc.EmbeddedXADataSource</config-property>
    <config-property name="property" type="java.lang.String">createDatabase=create</config-property>
    <config-property name="property" type="java.lang.String">databaseName=${jboss.server.data.dir}/derby-nuxeo</config-property>
    <config-property name="property" type="java.lang.String">user=sa</config-property>
    <config-property name="property" type="java.lang.String">password=</config-property>
    <max-pool-size>20</max-pool-size>
  </tx-connection-factory>
</connection-factories>

For PostgreSQL, adapt the datasource properties like this. Note the
track-connection-by-tx element which is needed as the PostgreSQL JDBC adapter
doesn't implement JTA fully.

<?xml version="1.0"?>
<connection-factories>
  <tx-connection-factory>
    <jndi-name>NXRepository/default</jndi-name>
    <adapter-display-name>Nuxeo SQL Repository DataSource</adapter-display-name>
    <rar-name>nuxeo.ear#nuxeo-core-storage-sql-ra-1.4.3-SNAPSHOT.rar</rar-name>
    <connection-definition>org.nuxeo.ecm.core.storage.sql.Repository</connection-definition>
    <xa-transaction/>
    <config-property name="name">default</config-property>
    <config-property name="xaDataSource" type="java.lang.String">org.postgresql.xa.PGXADataSource</config-property>
    <config-property name="property" type="java.lang.String">ServerName=localhost</config-property>
    <config-property name="property" type="java.lang.String">PortNumber/Integer=5432</config-property>
    <config-property name="property" type="java.lang.String">DatabaseName=nuxeo</config-property>
    <config-property name="property" type="java.lang.String">User=postgres</config-property>
    <config-property name="property" type="java.lang.String">Password=</config-property>
    <max-pool-size>20</max-pool-size>
    <track-connection-by-tx>true</track-connection-by-tx> 
  </tx-connection-factory>
</connection-factories>

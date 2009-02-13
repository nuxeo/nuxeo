Nuxeo repository SQL connector installation documentation

1. Install nuxeo

Download the latest 5.2 snapshot, for instance
http://www.nuxeo.org/static/snapshots/nuxeo-ep-5.2.0.SNAPSHOT-installer-20080810.jar
Install it in a directory called $JBOSS in this document.

2. Remove the old JCR connector

rm $JBOSS/server/default/deploy/nuxeo.ear/config/default-repository-config.xml
rm $JBOSS/server/default/deploy/nuxeo.ear/config/default-versioning-config.xml
rm $JBOSS/server/default/deploy/nuxeo.ear/system/nuxeo-core-jcr-connector-1.5-SNAPSHOT.jar
rm $JBOSS/server/default/deploy/nuxeo.ear/system/nuxeo-core-jca-1.5-SNAPSHOT.rar
  
3. Install the SQL connector

cp nuxeo-core-storage-sql-ra-1.5-SNAPSHOT.rar $JBOSS/server/default/deploy/nuxeo.ear/system
cp nuxeo-core-storage-sql-1.5-SNAPSHOT.jar $JBOSS/server/default/deploy/nuxeo.ear/system

4. Configure the SQL connector

4.1. Nuxeo extension point configuration

Copy doc/examples/repository-config.xml to
$JBOSS/server/default/deploy/nuxeo.ear/config/repository-config.xml

This extension point will in the future be made extensible to specify various
configuration options, for now the binaries storage is hardcoded to
$JBOSS/server/default/data/NXRuntime/binaries.

4.2. Datasources

Install the appropriate JDBC driver in $JBOSS/server/default/lib, for instance
derby-10.4.1.3.jar, or postgresql-8.2-507.jdbc3.jar.

Create the datasource file in $JBOSS/server/default/deploy/nuxeo.ear/datasources,
its name has to end in ...-ds.xml.

- for Derby, adapt the file doc/examples/repository-derby-ds.xml.

- for PostgreSQL, adapt the file doc/examples/repository-postgresql-ds.xml.
  Note the track-connection-by-tx element which is needed as the PostgreSQL
  JDBC adapter doesn't implement JTA fully.

Note that the datasource refers by full name to the name of the Rar using the
rar-name element. If you rename the Rar, the datasource has to be changed too.


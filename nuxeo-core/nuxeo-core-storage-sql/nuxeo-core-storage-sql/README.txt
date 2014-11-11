Nuxeo repository SQL connector installation documentation

1. Remove old JCR backend configuration if present

rm $JBOSS/server/default/deploy/nuxeo.ear/config/default-repository-config.xml
rm $JBOSS/server/default/deploy/nuxeo.ear/config/default-versioning-config.xml

2. Nuxeo extension point configuration

Copy doc/examples/repository-config.xml to
$JBOSS/server/default/deploy/nuxeo.ear/config/default-repository-config.xml.

This extension point will in the future be made extensible to specify various
configuration options, for now the binaries storage is hardcoded to
$JBOSS/server/default/data/NXRuntime/binaries.

You can enable clustering using:
  <clustering enabled="true" delay="1000" />
The delay is in milliseconds, and specifies the delay before which a node
doesn't check invalidations from other nodes. The default, 0, means checking
invalidations for every transaction started, which is costly.

You can configure the fulltext indexing analyzer using:
  <indexing>
    <fulltext analyzer="..."/>
  </indexing>
The specific syntax of the analyzer depends on the database used.
For H2, it's a lucene analyzer class.
  <fulltext analyzer="org.apache.lucene.analysis.fr.FrenchAnalyzer"/>
For PostgreSQL, it's a text search configuration.
See http://www.postgresql.org/docs/8.3/static/textsearch-configuration.html
  <fulltext analyzer="french"/>
For Microsoft SQL Server, it's a language. You can also specify a fulltext catalog,
the default is "nuxeo".
See http://msdn.microsoft.com/en-us/library/ms187317(SQL.90).aspx
  <fulltext analyzer="french" catalog="nuxeo"/>

3. Datasource configuration

Install the appropriate JDBC driver in $JBOSS/server/default/lib, for instance
derby-10.4.1.3.jar, or postgresql-8.2-507.jdbc3.jar.

Create the datasource file in
$JBOSS/server/default/deploy/nuxeo.ear/datasources/default-repository-ds.xml,
(its name is arbitrary but it has to end in ...-ds.xml)

- for Derby, adapt the file doc/examples/repository-derby-ds.xml.

- for PostgreSQL, adapt the file doc/examples/repository-postgresql-ds.xml.

- etc.

Note that this is a generic JCA connector datasource, so the syntax is different
from other JDBC datasources.

Note the track-connection-by-tx element which is always needed in Nuxeo so that multiple
core sessions opened in the same transaction can see each other's data.

IMPORTANT NOTE: the datasource refers by full name to the RAR using the
rar-name element. If you rename the RAR, the datasource has to be changed too.

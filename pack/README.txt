This script is building a zipped Nuxeo EAR given an input JBoss distribution.

To build the zipped EAR on the default jboss distribution simply run:

mvn package

The zipped nuxeo.ear will be put inside target/nuxeo.ear

To customize the nuxeo.ear zip just provide a properties files with custom settings.
See defualt.properties for the default property values. You can override any property specified in the default file.
The most important properties are:
NX_DISTRIBUTION - the complete artifact name for the target distribution to use
nuxeo.config.dir - the location where Nuxeo config directory will reside on production server
nuxeo.home.dir - the location where Nuxeo home directory will reside on production server
nuxeo.templates - the template ID to use when configuring Nuxeo.

See the example/pack.properties for a custom property file.

To build using a custom property file run:

mvn -o package -Dpack.config=example/pack.properties


To run the zipped EAR you must define any system property referenced inside the EAR configuration files.
For this you can use the JBoss system property service (see properties-service.xml in deploy directory).
The files that may reference a system property are:
nuxeo-ds.xml
META-INF/default-repository-ds.xml
META-INF/nuxeo-structure.xml

To load a properties file into the system properties add an entry like this in properties-service.xml
    <attribute name="URLList">
      ${jboss.server.config.url}/nuxeo.properties
    </attribute>

You can find in example/nuxeo.properties some properties you must usually define.

#nuxeo-ds.xml
nuxeo.data.dir
nuxeo.db.name
nuxeo.db.user
nuxeo.db.password

#default-repository-ds.xml
nuxeo.vcs.max-pool-size


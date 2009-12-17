Multi threaded document importer from server filesystem
=======================================================

The file importer comes as a Java library (with nuxeo runtime service)
and a sample JAX-RS interface to launch, monitor and abort import jobs.

Build with::

 $ mvn install -Dmave.test.skip=true

And deploy the two jars from the target subfolder into the nuxeo.ear/plugins or
nxserver/bundles folder of you nuxeo server (and restart).

To import the folder '/home/ogrisel/Documents' into the workspace
'/default-domain/workspaces/my-workspace' while monitoring the import logs from
a REST client use the following HTTP GET queries::

 GET http://localhost:8080/nuxeo/site/fileImporter/logActivate

 GET http://localhost:8080/nuxeo/site/fileImporter/run?targetPath=/default-domain/workspaces/my-workspace&inputPath=/home/ogrisel/Documents&batchSize=10&interactive=false&nbThreads=4

 GET http://localhost:8080/nuxeo/site/fileImporter/log


Multi threaded document importer from server filesystem
=======================================================

The file importer comes as a Java library (with nuxeo runtime service)
and a sample JAX-RS interface to launch, monitor and abort import jobs.

Build with::

  $ mvn install -Dmaven.test.skip=true

And deploy the two jars from the target subfolder into the nuxeo.ear/plugins or
nxserver/bundles folder of your nuxeo server (and restart).

To import the folder '/home/ogrisel/Documents' into the workspace
'/default-domain/workspaces/my-workspace' while monitoring the import logs from
a REST client use the following HTTP GET queries::

  GET http://localhost:8080/nuxeo/site/fileImporter/logActivate

  GET http://localhost:8080/nuxeo/site/fileImporter/run?targetPath=/default-domain/workspaces/my-workspace&inputPath=/home/ogrisel/Documents&batchSize=10&interactive=false&nbThreads=4

  GET http://localhost:8080/nuxeo/site/fileImporter/log

To execute those HTTP queries you can either use a browser with an active Nuxeo
session (JSESSIONID cookie) or use a third party stateless HTTP client with HTTP
Basic Authentication, eg: with the curl commandline client::

  $ curl --basic -u 'Administrator:Administrator' "http://localhost:8080/nuxeo/site/fileImporter/log"

Don't forget to quote the URL if it includes special shell characters such as
'&'.

You can also the generic HTTP GUI client from the rest-client java project::

  http://code.google.com/p/rest-client/

Don't forget to fill in the 'Auth' tab with your user credentials.


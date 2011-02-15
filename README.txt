Case Management Document Routing Addon
----------------------------------------

This addon adds the "routing" functionality to Nuxeo.
A "route" is a list of steps that a list of documents will execute. 
Steps can be:

    * distribute the documents
    * modify some metadata
    * create task for the document to be reviewed

See: https://doc.nuxeo.com/display/CMDOC/Document+Routing

Build with::

  $ mvn clean install 

And deploy the three jars from the target subfolders into the nuxeo.ear/plugins or
nxserver/bundles folder of your nuxeo server (and restart).


Document Routing Addon
----------------------------------------

This addon adds the "routing" functionality to Nuxeo.
A "route" is a workflow of type 'graph' that a list of documents will execute.
Steps can be:

    * distribute the documents
    * modify some metadata
    * create task for the document to be reviewed

See: http://doc.nuxeo.com/x/OwzF

Build with::

  $ mvn clean install

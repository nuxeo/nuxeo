About
-----

This module should be used to build nuxeo products.

The created artifact are not suitable to be published online since a build
project may generate different ZIP artifacts depending on the profile used.

Building
--------

Using this project you can build the following applications:

1. Nuxeo Shell 

  A client application suitable to connect on remote nuxeo servers.
  This application can be used to connect to remote nuxeo servers for debugging,
  browsing or administration stuff.

  Build syntax: `mvn package` or `mvn package -P shell`
    
2. Nuxeo Core Server 

  A minimal server application. An embedded repository will be started. No other
  platform services are available.
  
  This application can be used to debug, test or develop nuxeo components that
  need a repository connection.
  
  Remoting will be also available in the future via Nuxeo Runtime.
  
  Build syntax: `mvn package -P core`
    
3. Nuxeo Jetty Server
  
  A Nuxeo server application embedding a Jetty server. 
  
  This application can be used to browse repository content via WEB.
  
  This is also known as Nuxeo WebEngine (based on Jetty).

  Build syntax: `mvn package -P jetty`
   
4. Nuxeo GF3 Server
  
  A Nuxeo server application embedding a GlassFish v3 server. 
  
  This application can be used to browse a repository content via the web.
  
  This is also known as Nuxeo WebEngine (based on GF3).
   
  In the future, this application will provide a full installation of Nuxeo
  (including EJBs, JMS, etc).

  Build syntax: `mvn package -P gf3`

This module should be used to build nuxeo products. 
The created artifact are not suitable to be published online since a build project may generate 
different ZIP artifacts depending on the profile used.

Using this project you can build the following applications:

1. Nuxeo Shell 
    A client application suitable to connect on remote nuxeo servers.
    This application can be used to connect to remote nuxeo servers for debuging, browsing or administration stuff.

    Build syntax: `mvn package` or `mvn package -P shell`
    
2. Nuxeo Core Server 
    A minimal server application. An embedded repository will be started. No other platform services are available
    This application can be used to debug, test ot develop nuxeo components that needs a reposiotry connection.
    Remoting will be also available in future via Nuxeo Runtime.
    
    Build syntax: `mvn package -P core`
    
3. Nuxeo Jetty Server
   A nuxeo server application embeding a Jetty server. 
   This application can be used to browse repository content via WEB.
   This is also known as Nuxeo WebEngine (based on Jetty).

   Build syntax: `mvn package -P jetty`
   
4. Nuxeo GF3 Server
   A nuxeo server application embeding a GlassFish3 server. 
   This application can be used to browse repository content via WEB.
   This is also known as Nuxeo WebEngine (based on GF3).
   
   In future this application will provide a full installation of Nuxeo (including EJBs, JMS etc)

   Build syntax: `mvn package -P gf3`
    
   
   
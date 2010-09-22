About
=====

This is a prototype WebDAV extension to Nuxeo using JAX-RS and the webdav-jaxrs
extension (https://webdav.dev.java.net/webdav-jaxrs/).

Building / Running
------------------

Type: "make run". This builds and starts an embedded server with a demo
repository. 

You can also start the embedded server from your IDE (Eclipse or IDEA).

To deploy to a Tomcat Nuxeo distribution, edit the Makefile to enter the proper
location of your Nuxeo server, then "make deploy-tomcat".

If you want to debug on Tomcat, you may want to change the priority for category
"org.nuxeo" to DEBUG in the lib/log4j.xml file in the Nuxeo server
distribution.

Testing the embedded server
---------------------------

Type "run.sh" then:

1. Using Cadaver

Type: "cadaver http://localhost:9998/dav/workspaces"

2. Using Litmus:

Type: "litmus http://localhost:9998/dav/workspaces"

3. Using the Mac

On your Finder, type "Cmd-K" (Or Menu -> "Go" -> "Connect to server") and enter
"cadaver http://localhost:9998/dav/workspaces"

4. Usng Windows:

Not tested for now.

Testing the tomcat server
-------------------------

Start the Nuxeo server, then use the same information as above, except for the
URL: http://localhost:8080/nuxeo/site/dav/default-domain/workspaces


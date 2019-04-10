About
=====

This is a prototype WebDAV extension to Nuxeo using JAX-RS and the webdav-jaxrs
extension (https://webdav.dev.java.net/webdav-jaxrs/).

Building / Running
------------------

Type: "make run". This builds and starts an embedded server with a demo
repository (you may need to do it twice if it fails the first time). 

You can also start the embedded server from your IDE (Eclipse or IDEA).

To deploy to a JBoss or Tomcat Nuxeo distribution, edit the Makefile to enter
the proper location of your Nuxeo server, then "make deploy-jboss" or "make
deploy-tomcat".

If you want to debug on Tomcat, you may want to change the priority for
category "org.nuxeo" to DEBUG in the lib/log4j2.xml file in the Nuxeo server
distribution.

Testing the embedded server
---------------------------

Type "run.sh" then:

1. Using Cadaver

Type: "cadaver http://localhost:9998/dav/workspaces"

2. Using Litmus:

Type: "litmus http://localhost:9998/dav/workspaces"

3. On Mac OS

On your Finder, type "Cmd-K" (Or Menu -> "Go" -> "Connect to server") and enter
"cadaver http://localhost:9998/dav/workspaces"

4. On Linux

Make sure you have a directoy in /mnt/dav

Type (as root): mount.davfs http://localhost:9998/dav/workspaces /mnt/dav

5. On Windows

Not tested for now.

Testing the tomcat server
-------------------------

Start the Nuxeo server, then use the same information as above, except for the
URL: http://localhost:8080/nuxeo/site/dav/default-domain/workspaces

More tests
----------

Assuming that you have mounted the the DAV folder on your OS, you can run several
more test / benchmarks suites from the benchmarks/ directory:

- the benchmarks/test.py test suite (edit the script to reflect your mount
  point)

- the postmark suite: 
  1. compile it (gcc postmark*.c), 
  2. run a.out,
  3. set the location to your mount point, for ex: set location /Volumes/workspaces
  4. run



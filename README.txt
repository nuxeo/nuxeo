Nuxeo Digital Asset Management application
==========================================

Nuxeo DAM is a Seam / JSF web application that leverages the Nuxeo Enterprise
Platform to build a multimedia document collection management application.

Building and deploying on an existing JBoss
-------------------------------------------

To build and deploy you first need to set up a JBoss 4.2.3.GA instance.

Then, you configure the build.properties files (starting from the
build.properties.sample file to be found in the current folder), to point you
JBoss instance::

  $ cp build.properties.sample build.properties
  $ vi build.properties

You can then build Nuxeo DAM with::

  $ ant deploy

TODO: add here instructions for Seam and nuxeo.war hot redeployment.

Building a full archive
-----------------------

You can also build a complete JBoss or Tomcat distribution with the DAM
application included, using the following maven commands::

  $ mvn install -Dmaven.test.skip=true
  $ cd nuxeo-dam-distribution
  $ mvn install -Dmaven.test.skip=true -Ptomcat

The generated zip should be available in::

  nuxeo-dam-distribution/nuxeo-dam-distribution-tomcat/target/nuxeo-dam-distribution-tomcat-X.X-SNAPSHOT.zip

After unzipping, make the Tomcat start scripts runnable and launch Tomcat::

  $ cd nuxeo-dam-tomcat
  $ chmod a+x bin/*.sh
  $ ./bin/catalina.sh start


Technical Overview
------------------

The doc/ folder holds a technical overview of the design goals and technical
architecture of the nuxeo-dam application.


Running the functionnal test suite
----------------------------------

A functional test suite based on selenium is available in nuxeo-dam-ear. Please
follow the instructions in nuxeo-dam-ear/ftest/selenium/README.txt .


Login page Copyrights
----------------------------------

The fish photography has been taken by Luc Viatour. It is under GFDL and
Creative Commons licences:
email: l.viatour@mm.be
website: http://www.lucnix.be/main.php

Picture colors has been edited by Michael Yucha.
flickR: http://www.flickr.com/photos/greenwenvy/


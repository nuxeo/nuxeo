Nuxeo Digital Asset Management application
==========================================

nuxeo-dam is a Seam / JSF web application that leverage the Nuxeo Enterprise
Platform to build a multimedia document collection application.

Building and deploying
----------------------

To build and deploy you need a jboss 4.2.3.GA instance setup and configured in
a build.properties files based on the build.properties.sample file to be found
in the current folder::

  $ cp build.properties.sample build.properties
  $ vim build.properties

Build with::

  $ ant deploy

TODO: add here instructions for Seam and nuxeo.war hot redeployment.


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

The fish photography has been taken by Luc Viatour. It is under GFDL and Creative Commons licences
email: l.viatour@mm.be
website: http://www.lucnix.be/main.php

Picture colors has been edited by Michael Yucha.
flickR: http://www.flickr.com/photos/greenwenvy/


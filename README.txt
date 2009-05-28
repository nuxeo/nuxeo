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

  $ and deploy

TODO: add here instructions for Seam and nuxeo.war hot redeployment.


Running the functionnal test suite
----------------------------------

A functional test suite based on selenium is available in nuxeo-dam-ear. Please
follow the instructions in nuxeo-dam-ear/ftest/selenium/README.txt .


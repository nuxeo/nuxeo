# Nuxeo Digital Asset Management 

Nuxeo DAM is a Seam / JSF web application that leverages the Nuxeo Enterprise
Platform to build a multimedia document collection management application.

## Building a full distribution

You can build a complete Tomcat or JBoss distribution with the Nuxeo DAM
application included, using the following commands:

    $ ant assemble-tomcat 

or

    $ ant assemble-jboss

The generated zip should be available in:

    nuxeo-dam-distribution/target/nuxeo-dam-distribution-X.X-SNAPSHOT-tomcat.zip

or

    nuxeo-dam-distribution/target/nuxeo-dam-distribution-X.X-SNAPSHOT-jboss.zip

After unzipping, make the `nuxeoctl` script runnable and launch Nuxeo DAM:

    $ cd nuxeo-dam-X.X-SNAPSHOT-tomcat

or

    $ cd nuxeo-dam-X.X-SNAPSHOT-jboss
    $ chmod a+x bin/nuxeoctl
    $ ./bin/nuxeoctl start


## Building and deploying on an existing JBoss

You can also deploy Nuxeo DAM on an existing JBoss 5 instance.

Configure the build.properties files (starting from the
`build.properties.sample` file to be found in the current folder), to point your
JBoss or Tomcat instance:

    $ cp build.properties.sample build.properties
    $ vi build.properties

You can then build and deploy Nuxeo DAM with:

    $ ant deploy-ear-jboss


## Technical Overview 

A technical overview can be found here:
  <http://doc.nuxeo.com/display/DAMDOC/Nuxeo+DAM+Developer+documentation>


## Running the functionnal test suite

A functional test suite based on selenium is available in `nuxeo-dam-distribution`. Please
follow the instructions in `nuxeo-dam-distribution/ftest/selenium/README.txt` .


## Login page Copyrights 

The fish photography has been taken by Luc Viatour. It is under GFDL and
Creative Commons licences:
email: l.viatour@mm.be
website: <http://www.lucnix.be/main.php>

Picture colors has been edited by Michael Yucha.
flickR: <http://www.flickr.com/photos/greenwenvy/>

================================
nuxeo-distribution-izpack
================================

This tool converts a Nuxeo EP zip archive (like the one produced by
``nx-builder package``) into a java installer using IzPack.  

How to use it?
--------------

* Edit the build.properties and set the zip file path.

* Check that pom.xml parent version points to the exact version you are
  releasing.

* Update the src/izpack/README.html to reflect the release information.

* Build the jar:

   mvn clean package


About
-----

This module builds Nuxeo products.

Building predefined applications
--------------------------------
On 5.1 branch, only nuxeo-ep 5.1 and associated nuxeo-shell are available.

From nuxeo-ecm root, all Ant commands are preserved: run "ant -projecthelp" for available targets list.

1) Nuxeo EP
Previously built by nuxeo-platform/nuxeo-platform-ear/, Nuxeo EP EAR is now built here.
From this directory, run "mvn package".
From nuxeo-platform-ear, run "mvn package" or see in package.sh for available packages.
Built EAR is in nuxeo-platform-ear/target/ and its name depends on choosen package: default is nuxeo.ear
Setup and tests
===============

1) Create a recent Nuxeo WebEngine GF3 or Jetty instance in nxserver/

(or a symlink to an instance).

2) Type "make"

3) You're done.

What it does
============

1) Build a nuxeo-webdav-xyz-SNAPSHOT.jar

2) Deploys it to the server (in bundles/)

3) Deploys all the needed dependencies to the server (in lib/)

Using with Nuxeo IDE
====================

1) Type "mvn eclipse:eclipse"

2) Remove the nuxeo-webdav-xyz-SNAPSHOT.jar from nxserver/bundles (you can't
have both)

3) In Eclipse: import project; give it the Nuxeo WebEngine Nature; create the
server; your're done. (More info on http://www.nuxeo.org/webengine/)

How to debug (without Nuxeo IDE)
================================

1) Type "make stop run" -> this starts jetty in debug mode.

2) Connect your IDE to the debug port 8788.


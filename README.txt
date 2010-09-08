Nuxeo-Platform-Signature: The digital signature plugin for PDF files.

Random notes about this project:

DEPLOYMENT:

PREREQUISITES:
The iText library needs to be copied into the library folder of your app server:
Example using jboss:
Copy this file:  itext-2.0.7.jar
into the following directory: <jboss>/server/default/deploy/nuxeo.ear/lib

Optimally this operation will be scripted for the deployment phase.
----

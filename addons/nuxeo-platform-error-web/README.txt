========================
Nuxeo Platform Error web
========================

Addon performing errors on the web side for automated testing of the
exception handling system.

Allows to tests JSF/Seam/Webengine errors, as well as rollbacks when issuing
an error after document creation. Also allows to test redirection mechanism
when anonymous plugin is used and security exceptions are thrown.

Selenium tests make sure that the right exception page is shown. Error logs
should be checked too, in case the exception handling triggers additional
errors that should not be there.


Install
-------

Setup properties in a file called "build.properties" according to you needs
and run "ant deploy" to deploy on a jboss with nuxeo installed.

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

Put generated jar nuxeo-platform-error-web to your nxserver/bundles
directory and start the server.

Launch selenium tests
---------------------

Run:

    $ mvn clean install -Pitest

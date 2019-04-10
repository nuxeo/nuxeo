Case Management selenim tests
=============================

Run
---

Selenium tests can be run executing the run.sh script. All defined suites
will be launched.

Configuration
-------------

Some configuration is available on run.conf file. To be able to deal with
file upload, we need a firefox profile with specific security settings. It
has to state the server url, so http://localhost:8080 is currently hardcoded
in ffprofile/prefs.js.

Prerequisites
-------------

These tests are designed to be launched on a default Case Management instance
with the following configuration

- default profile configuration
- jboss server started on http://localhost:8080


Automated testing
-----------------

Automated testing as to be done with the following steps:

- setup a jboss 4.2.3 patched
- deploy project on it (ant deploy)
- execute run similarly to what's done in run.sh script.

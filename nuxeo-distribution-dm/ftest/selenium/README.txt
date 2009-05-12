==================
Selenium Nuxeo EP
==================

Requirements
===============

* firefox 2 or 3

* xvfb for HIDE_FF mode using fake X server


Running test suites
=====================

* Default suite1 + suite2 on localhost

  ./run.sh

* Default suites on a remote site

  URL=http://server:8080/nuxeo ./run.sh

* Custom suites on localhost

  SUITES="suite-publication suite-jbpm" ./run.sh

* Run without a X server hiding firefox

  HIDE_FF=true ./run.sh

You can setup a run.conf file with your default options, look at
run.conf.sample.

Note that run.sh update the user-extensions.js and ffprofile/pref.js.

Test suites description
=========================

TODO: describes suites, accounts created, documents layouts ...

=================
Selenium Nuxeo EP
=================

Requirements
============

* firefox 2 or 3

* xvfb for HIDE_FF mode using fake X server


Running test suites
===================

* Default suite1 + suite2 on localhost

  ./run.sh

* Default suites on a remote site

  URL=http://server:8080/nuxeo ./run.sh

* Custom suites on localhost

  SUITES="suite-publication suite-jbpm" ./run.sh

* Run without a X server hiding firefox

  HIDE_FF=true ./run.sh


If firefox is not hidden, an application will open providing the tests suite
with an embedded firefox navigator. Tests can be stopped, restarted,
etc,... Breakpoints can be added clicking on a test or a line of the test.

You can setup a configuration file with your default options, look at
run.conf.sample. Copy it to a file named 'run.conf' for these parameters to
be taken into account.

The run.sh creates or updates the ffprofile/pref.js and user-extensions.js
files.

ffprofile/prefs.js holds the base url. It should be used/changed in command
line option (not useful when using Selenium IDE).
It also holds the current language (en). When using Selenium IDE, make sure
english is your default language.

user-extensions.js.sample holds the current folder absolute path. It should
be copied to user-extensions.js and modified accordingly when launching the
suite via Selenium IDE. Via command line, replacement is done in the run.sh
script.


Writing tests
=============

Selenium documentation
----------------------

Helpful links: http://seleniumhq.org/documentation/

Commands reference: http://seleniumhq.org/projects/core/reference.html

Firefox plugins
---------------

Here are some Firefox plugins that make life easier writing Selenium tests:

- Selenium IDE (http://seleniumhq.org/projects/ide/) can be helpful writing
  a single test before adding it to a suite.

- XPath Checker (https://addons.mozilla.org/fr/firefox/addon/1095) is very
  handy when struggling to find an element path in the HTML page DOM
  structure. Higly recommended!

Structure of Nuxeo test suites
------------------------------

The Nuxeo test suites are located in subdirectory tests/ and named
tests.html, suite1.html and suite2.html.

tests.html has been cut in two for automated testing: THINK OF UPDATING ALL
FILES WHEN ADDING/REMOVING tests.

Some new suites have been defined for the 5.2 branch as interface has
changed: tests-5.2.html, suite1-5.2.html, suite2-5.2.html. These suites may
reference tests common to both suites, as well as specific tests for
interfaces that have changed. Tests will usually follow the same naming
convention (for instance, manageRightsUsersByAdmin.html on 5.1 and
manageRightsUsersByAdmin-5.2.html on 5.2).

Note that the tests are currently not very "unit": some tests will require
previous tests to be run to succeed.


Generic advice when writing tests
---------------------------------

- Be specific when testing an element: a vague xpath reference may trigger a
  false error if the HTML page slightly changes. If you can't be specific,
  then page rendering should be changed to ease this process.

- Handle ajax requests properly: using the Selenium command
  "waitForCondition" with appropriate javascript testing is usually enough
  to test that the ajax response has been received. If this condition is not
  set, or not appropriate, it may lead to "heisenbugs", e.g. tests
  alternatively failing or succeeding with no apparent reason.


Test suites description
=======================

TODO: describes suites, accounts created, documents layouts ...

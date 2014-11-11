======================================
Funkload Nuxeo EP jboss test scripts
======================================

Abstract
============

This module contains python funkload scripts. These scripts can be used
to do functional testing or benching of the default Nuxeo EP web interface
including rest api implemented in nuxeo-platform-ui-web.


Script example
==============

Testing rest api
-----------------

Using the RestAPI helper class::

 class MySuite(FunkLoadTestCase):
     def testRest(self):
         r = RestAPI(self)
         r.login('Administrator', 'Administrator')
         root_uid = r.getRootWorkspaceUid()         # get the root uid
         ws_uid = r.createDocument(root_uid,
                                   'Workspace', 'a rest workspace',
                                   'A description')
         r.logout()

Visit test_rest.py for more examples.

Testing the web interface
---------------------------

These test use a kind of `PageObject
<http://code.google.com/p/webdriver/wiki/PageObjects>`_ and `fluent
interface <http://www.martinfowler.com/bliki/FluentInterface.html>`_
patterns.

Writing a new scenario can be done like this::

 class MySuite(NuxeoTestCase):
    def testMyScenario(self):
        (LoginPage(self)
         .login('Administrator', 'Administrator')
         .getRootWorkspaces()
         .createWorkspace('My workspace', 'Test ws')
         .rights().grant('ReadWrite', 'members')
         .view()
         .createFolder('My folder', 'Test folder')
         .createFile('My file', 'Test file', 'foo.pdf')
         .getRootWorkspaces().deleteItem("My workspace")
         .logout())


Visit test_pages.py for more examples.

Layout
=======

* nuxeo/rest.py

  RestAPI class helper.

* nuxeo/pages.py

  The Pages class which are simple abstraction of the web app, the main
  difference with web driver PageObjects is that they contains assertion to
  ease the fluent interface.

* nuxeo/testcase.py

  Simple nuxeo test case that use an xmlrpc server to get credential to
  access the nuxeo ep.

* test_rest.py, Rest.conf

  Test suite to check rest api (nuxeo/rest.py):
  Create a workspace, a folder and few documents, ExportTree on few documents.

* test_pages.py, Page.conf

  A test suite to check the pages api (nuxeo/pages.py). Note that the test
  suite will create a new member account named flnxtest.

* test_nuxeo.py, Nuxeo.conf

  A test suite used to test/bench a Nuxeo EP. This test will create many
  members accounts (the ones present in the password.txt files) and will add
  a new workspace and folder with few file documents.

* credential.conf, passwd.txt, groups.txt

  Credential xmlrpc configuration with unix like passwd/groups files used by
  nuxeo testcase to get credentials.

* monitor.conf

  Configuration of the local monitor server used by the bench runner.

Running tests
===============


Requirement
-------------

* Install latest FunkLoad snapshots, on Lenny or Intrepid Ibex this can be
  done like this::

  sudo aptitude install python-dev python-xml python-setuptools \
       python-webunit=1.3.8-1 python-docutils gnuplot
  sudo aptitude install tcpwatch-httpproxy --without-recommends
  sudo easy_install -f http://funkload.nuxeo.org/snapshots/ -U funkload


For other distro visit http://funkload.nuxeo.org/INSTALL


Running test
-------------

Using a makefile

* make
  this is equivalent to "make start rest page test-nuxeo stop"

* make start
  Start local monitoring
  Start the xmlrpc credential server to serve user/password to test runner.
  Check if nuxeo ep login page is accessible.

* make stop
  Stop monitoring, credential server.

* make rest
  Run  the test_rest suite.

* make page
  Run the test_pages suite.

* make test-nuxeo
  Run the test_nuxeo suite.

To run test on remote site use the URL option:

  make rest URL=http://host.to.nuxeo:8080/nuxeo

To pass extra options to the funkload test runner using EXT:

  make page EXT="-dV"

  This will run the test in debug mode and try to ouput response page in a
  running Firefox (See fl-run-test -h for more information).

Note that log files are located in the target/ftest/funkload of the
nuxeo-distribution-dm.

Benching
---------

* make bench
  Full bench: init users and test layout, bench writer, bench reader.

  This will produce an html report located in
  ../../target/ftest/funkload/report

* make bench-reader
  Bench the reader part only.



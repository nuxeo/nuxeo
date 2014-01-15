======================================
Funkload Nuxeo DM jboss test scripts
======================================

Abstract
============

This module contains python funkload scripts. These scripts can be used
to do functional testing or benching of the default Nuxeo DM web interface
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

  A test suite used to test/bench a Nuxeo DM. This test will create many
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



Nuxeo DM running on JAVA 5
--------------------------

Starting with Nuxeo DM 5.3.0 the default JVM is Sun Java 6.

Scripts must be adapted to use JAVA5.

Memory and PostgreSQL analysis per page
------------------------------------------

By install JMXSH and having access to the PostgreSQL log file, the Nuxeo
test case can collect and extract meaningful metrics for each page.

Requirement:

* Install JMXSH from http://code.google.com/p/jmxsh/ and make
the jar available at /usr/local/jmxsh/jmxsh-R5.jar or set a JMXSH
environment variable.

* Install pgfouine, gawk

* Enable JMX access by setting the java option in the nuxeo.conf

   JAVA_OPTS=$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=1089 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false

* Edit the FunkLoad Nuxeo.conf file and set the following path:

pglog=/path/to/postgresql/log/postgresql-8.4-main.log
log_dir=../../target/ftest/funkload/log
monitorctl_file=/path/to/nuxeo-server/bin/monitorctl.sh

* To extract all the SQL queries you need to use the following
  PostgreSQL setup:

     lc_messages = 'en_US.UTF-8'
     log_line_prefix = '%t [%p]: [%l-1] '
     log_min_duration_statement = 0

Usage:

Run your test, the FunkLoad Nuxeo test case will perform a Full GC and
a heap history before each request and will save all SQL queries and
heap history after.

You can generate the pgfouine reports per page like this:
 
for f in `ls pg-test*.log`; do pgfouine -file $f -logtype stderr -top 30 > ${f%log}html; done

You can view the amount of memory needed by page like this:     

for f in `ls hh-test*-before.txt`; do e=${f%-before.txt}-end.txt; after=`grep Total $e | awk '{ print $3 }'`; before=`grep Total $f | awk '{ print $3 }'`; diff=`expr $after - $before`; hrdiff=`echo $diff | awk '{sum=$1;hum[1024**3]="Gb";hum[1024**2]="Mb";hum[1024]="Kb"; for (x=1024**3; x>=1024; x/=1024){ if (sum>=x) { printf "%.2f %s\n",sum/x,hum[x];break }}}'`; echo "${f%-before.txt} $before $after: $hrdiff"; done

Note that you need to use a huge heap to prevent any minor or major GC.

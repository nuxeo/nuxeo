About the Nuxeo EP Project
==========================

What is Nuxeo EP?
-----------------

Nuxeo EP is an open source platform for Enterprise Content Management.

See: <http://www.nuxeo.com/en/products/ep> for a list of features and
benefits.

See: <http://doc.nuxeo.com/display/MAIN/Getting+started+with+Nuxeo+--+a+beginner's+page>
for a short introduction to Nuxeo EP.

See: <http://en.wikipedia.org/wiki/Enterprise_content_management> for a
general definition of Enterprise Content Management.


How to compile the Nuxeo EP sources
-----------------------------------

### Short story

1. Make sure all the sources are checked out, as there are several subpackages
that need to be present to build Nuxeo:

  - nuxeo-common
  - nuxeo-core
  - nuxeo-dm
  - nuxeo-features
  - nuxeo-gwt
  - nuxeo-jsf
  - nuxeo-runtime
  - nuxeo-services
  - nuxeo-theme
  - nuxeo-webengine
  - nuxeo-distribution

  Under Linux/Unix/MacOS: run `./clone.sh`

  Under Windows: run `clone.bat`

  See <http://doc.nuxeo.org/xwiki/bin/view/FAQ/DownloadingNuxeoSources> for more
  information if needed.

2. Launch the build:

  You can do so by typing: "make build assemble-tomcat" (assuming you have Maven
  and Make installed on your system).

  Alternatively, if you only have Maven, you can type:

        mvn install -Dmaven.test.skip=true
        cd addons ; mvn install -Dmaven.test.skip=true
        cd ../nuxeo-distribution ; mvn clean install -Pnuxeo-dm,tomcat

  In both cases, you will get your tomcat-based build in the
  nuxeo-distribution/nuxeo-distribution-tomcat/target directory.

3. Run the tests:

  If you want to run the Selenium test suite, you can run: "make selenium-tomcat".

### Packaging Nuxeo EP from sources

Various pre-configured packages (various application servers and multiple
backends) are available for download from: <http://www.nuxeo.com/downloads>

In order to locally build Nuxeo EP, see nuxeo-distribution/README.txt

### Long(er) story

If the information above are not enough, please read from the Nuxeo Book the 
"Detailed Development Software Installation Instructions" annex:
<http://doc.nuxeo.org/current/books/nuxeo-book/html/dev-environment-installation.html>


Where to get help and get involved
----------------------------------

First, look at the documentation, on <http://doc.nuxeo.com>.

The Nuxeo Community Forum (<http://forum.nuxeo.com/>) is the place where
thousands of Nuxeo users gathers to exchange questions and answers,
information and tips. We have also a few mailing lists that mirror some
of the forums: <http://lists.nuxeo.com/>

If you've found a bug and want to suggest an improvement, you can use our
Jira issue tracker: <http://jira.nuxeo.org/>.

Last, if you need professional support for your critical application, we have
a subscription program: <http://www.nuxeo.com/en/subscription> that also
packages additional services.


How to contribute
-----------------

See this page for practical information:
<http://doc.nuxeo.com/display/NXDOC/Nuxeo+contributors+welcome+page>

This presentation will give you more insight about "the Nuxeo way":
<http://www.slideshare.net/nuxeo/nuxeo-world-session-becoming-a-contributor-how-to-get-started>


About Nuxeo
-----------

Nuxeo provides a modular, extensible Java-based
[open source software platform for enterprise contentmanagement](http://www.nuxeo.com/en/products/ep),
and packaged applications for [documentmanagement](http://www.nuxeo.com/en/products/document-management),
[digital asset management](http://www.nuxeo.com/en/products/dam) and 
[casemanagement](http://www.nuxeo.com/en/products/case-management).

Designed by developers for developers, the Nuxeo platform offers a modern
architecture, a powerful plug-in model and extensive packaging
capabilities for building content applications.

More information on: <http://www.nuxeo.com/>


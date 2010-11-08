How to compile Nuxeo EP sources
========================================

Short story
-----------

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

Under Linux/Unix/MacOS: run ./clone.sh

Under Windows: run clone.bat

See http://doc.nuxeo.org/xwiki/bin/view/FAQ/DownloadingNuxeoSources for more
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


Packaging Nuxeo EP from sources
-------------------------

Various pre-configured packages (various application servers and multiple
backends) are available for download from:

- http://maven.nuxeo.org/
- http://www.nuxeo.com/downloads/

In order to locally build Nuxeo EP, see nuxeo-distribution/README.txt


Long story
----------

If the information above are not enough, please read from the Nuxeo Book the 
"Detailed Development Software Installation Instructions" annexe:
http://doc.nuxeo.org/current/books/nuxeo-book/html/dev-environment-installation.html

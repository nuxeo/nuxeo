How to compile Nuxeo EP sources
========================================

Short story
-----------

1. Make sure all the sources are checked out, as this is a Mercurial forest,
i.e. there are several different trees that must be present in this directory,
notably:

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

2. Compile:

- Using Ant: "ant install" or using Maven: "mvn clean install -Dmaven.test.skip=true"

Packaging Nuxeo EP from sources
-------------------------

Various pre-configured packages (various application servers and multiple backends) 
are available for download from:
- http://maven.nuxeo.org/
- http://www.nuxeo.com/downloads/

In order to locally build Nuxeo EP, see nuxeo-distribution/README.txt

Long story
----------

If the information above are not enough, please read from the Nuxeo Book the 
"Detailed Development Software Installation Instructions" annexe:
http://doc.nuxeo.org/current/books/nuxeo-book/html/dev-environment-installation.html

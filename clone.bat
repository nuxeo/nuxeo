@echo off
rem This script will checkout the needed sources

set CV=release-1.6.2-I20100610_0115
set PV=release-5.3.2-I20100610_0115

hg clone -r %CV% http://hg.nuxeo.org/nuxeo/nuxeo-common
hg clone -r %CV% http://hg.nuxeo.org/nuxeo/nuxeo-runtime
hg clone -r %CV% http://hg.nuxeo.org/nuxeo/nuxeo-core

hg clone -r %PV% http://hg.nuxeo.org/nuxeo/nuxeo-services
hg clone -r %PV% http://hg.nuxeo.org/nuxeo/nuxeo-theme
hg clone -r %PV% http://hg.nuxeo.org/nuxeo/nuxeo-webengine
hg clone -r %PV% http://hg.nuxeo.org/nuxeo/nuxeo-jsf
hg clone -r %PV% http://hg.nuxeo.org/nuxeo/nuxeo-gwt
hg clone -r %PV% http://hg.nuxeo.org/nuxeo/nuxeo-features
hg clone -r %PV% http://hg.nuxeo.org/nuxeo/nuxeo-dm

hg clone -r %PV% http://hg.nuxeo.org/nuxeo/nuxeo-distribution

hg clone -r %PV% http://hg.nuxeo.org/addons ../nuxeo-addons

svn export https://svn.nuxeo.org/nuxeo/tools/mercurial .

hgx %PV% %CV% up -C
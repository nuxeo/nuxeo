# This script will check out the needed sources from mercurial.

CV=release-1.7.0-I20100830_1212 # Core version
PV=release-5.4.0-I20100830_1212 # Platform version

hg clone -r $CV http://hg.nuxeo.org/nuxeo/nuxeo-common
hg clone -r $CV http://hg.nuxeo.org/nuxeo/nuxeo-runtime
hg clone -r $CV http://hg.nuxeo.org/nuxeo/nuxeo-core

hg clone -r $PV http://hg.nuxeo.org/nuxeo/nuxeo-services
hg clone -r $PV http://hg.nuxeo.org/nuxeo/nuxeo-theme
hg clone -r $PV http://hg.nuxeo.org/nuxeo/nuxeo-webengine
hg clone -r $PV http://hg.nuxeo.org/nuxeo/nuxeo-jsf
hg clone -r $PV http://hg.nuxeo.org/nuxeo/nuxeo-gwt
hg clone -r $PV http://hg.nuxeo.org/nuxeo/nuxeo-features
hg clone -r $PV http://hg.nuxeo.org/nuxeo/nuxeo-dm

hg clone -r $PV http://hg.nuxeo.org/nuxeo/nuxeo-distribution

hg clone -r $PV http://hg.nuxeo.org/addons ../nuxeo-addons

svn export https://svn.nuxeo.org/nuxeo/tools/mercurial/.hgfunctions
. .hgfunctions

hgx $PV $CV up -C
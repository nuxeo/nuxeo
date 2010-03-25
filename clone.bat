rem This script will checkout the needed sources

hg clone -r 1.6.1 http://hg.nuxeo.org/nuxeo/nuxeo-common
hg clone -r 1.6.1 http://hg.nuxeo.org/nuxeo/nuxeo-runtime
hg clone -r 1.6.1 http://hg.nuxeo.org/nuxeo/nuxeo-core

hg clone -r 5.3.1 http://hg.nuxeo.org/nuxeo/nuxeo-services
hg clone -r 5.3.1 http://hg.nuxeo.org/nuxeo/nuxeo-theme
hg clone -r 5.3.1 http://hg.nuxeo.org/nuxeo/nuxeo-webengine
hg clone -r 5.3.1 http://hg.nuxeo.org/nuxeo/nuxeo-jsf
hg clone -r 5.3.1 http://hg.nuxeo.org/nuxeo/nuxeo-gwt
hg clone -r 5.3.1 http://hg.nuxeo.org/nuxeo/nuxeo-features
hg clone -r 5.3.1 http://hg.nuxeo.org/nuxeo/nuxeo-dm

hg clone -r 5.3.1 http://hg.nuxeo.org/nuxeo/nuxeo-distribution

hg clone http://hg.nuxeo.org/addons ../nuxeo-addons

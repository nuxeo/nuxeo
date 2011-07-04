@echo off
rem This script will checkout the needed sources

set VERSION=release-5.4.1-HF06

hg clone http://hg.nuxeo.org/nuxeo/nuxeo-common
hg clone http://hg.nuxeo.org/nuxeo/nuxeo-runtime
hg clone http://hg.nuxeo.org/nuxeo/nuxeo-core

hg clone http://hg.nuxeo.org/nuxeo/nuxeo-services
hg clone http://hg.nuxeo.org/nuxeo/nuxeo-theme
hg clone http://hg.nuxeo.org/nuxeo/nuxeo-webengine
hg clone http://hg.nuxeo.org/nuxeo/nuxeo-jsf
hg clone http://hg.nuxeo.org/nuxeo/nuxeo-gwt
hg clone http://hg.nuxeo.org/nuxeo/nuxeo-features
hg clone http://hg.nuxeo.org/nuxeo/nuxeo-dm

hg clone http://hg.nuxeo.org/nuxeo/nuxeo-distribution

hg clone http://hg.nuxeo.org/addons ../nuxeo-addons

svn export https://svn.nuxeo.org/nuxeo/tools/mercurial .

hgf pull
hgf up -C %VERSION%

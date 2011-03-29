# This script will check out the needed sources from mercurial.

VERSION=release-release-5.4.1-I20110302_0116-1

for x in nuxeo-common nuxeo-runtime nuxeo-core \
    nuxeo-services nuxeo-theme nuxeo-webengine nuxeo-jsf \
    nuxeo-gwt nuxeo-features nuxeo-dm \
    nuxeo-distribution
do
    if [ ! -e $x ]
    then
        hg clone https://hg.nuxeo.org/nuxeo/$x $x
    fi
done

if [ ! -e addons ]
then
    hg clone https://hg.nuxeo.org/addons addons
fi

. scripts/hgfunctions.sh

hgf up $VERSION
( cd addons ; hg up $VERSION )
( cd addons ; ./clone.py $VERSION )


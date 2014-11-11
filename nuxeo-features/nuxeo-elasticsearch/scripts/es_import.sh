#!/bin/bash

# Configure ES
ESHOST=${ESHOST:-localhost}
ESPORT=${ESPORT:-9200}
# number of concurrent importer
CONCURRENCY=${J:-4}

ESINDEX=${ESINDEX:-nuxeo}
ESTYPE=${ESTYPE:-doc}


TMPDIR=/tmp/bulk-$ESINDEX-$ESTYPE
rm -rf $TMPDIR
mkdir -p $TMPDIR || exit -1

ARCHIVE=$1
shift || exit 1
ARCHIVE=`readlink -e $ARCHIVE`

if [ -f $ARCHIVE ]; then
  cd $TMPDIR
  tar xzf $ARCHIVE || exit -2
  dir=$TMPDIR
else
  dir=$ARCHIVE
fi

echo "### Number of doc before import"
curl -XGET "$ESHOST:$ESPORT/$ESINDEX/$ESTYPE/_count"
echo

echo "### Total doc to import: " 
echo $((`find $dir -type f -print0 | xargs -0 cat | wc -l`/2))
echo "### Import ..." `date`
time parallel -j$CONCURRENCY curl -s -XPOST $ESHOST:$ESPORT/_bulk --data-binary {} -- `find $dir -type f | sed s/^/@/` >  /dev/null

sleep 2
echo "### Number of doc after import"  `date`
set -x
curl -XGET "$ESHOST:$ESPORT/$ESINDEX/$ESTYPE/_count"
set +x
echo

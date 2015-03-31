#!/bin/bash

QUERY="SELECT%20*%20FROM%20Document"
# note that Rest API don't accept bigger batch size than 100
BATCH_SIZE=100

NXUSER=${NXUSER:-Administrator}
NXPASSWORD=${NXPASSWORD:-Administrator}
NXURL=${NXURL:-http://localhost:8080/nuxeo}

ESINDEX=${ESINDEX:-nuxeo}
ESTYPE=${ESTYPE:-doc}
TMPDIR=/tmp/dump-$ESINDEX-$ESTYPE


CURL="curl -v -XGET -u $NXUSER:$NXPASSWORD -H Accept:application/json+esentity"

mkdir -p $TMPDIR || exit -1
cd $TMPDIR || exit -2

echo "### Dump Nuxeo documents ..." `date`
echo "### Page 0/?"
i=0
URL="$NXURL/api/v1/path/default-domain/@search?query=$QUERY&pageSize=$BATCH_SIZE&currentPageIndex=$i&maxResults=-1"
pages=`$CURL -o doc$i $URL 2>&1 | grep NXnumberOfPages | awk '{split($0,a,":"); print a[2]}' | tr -d ' \r'`
echo "### Found total pages: $pages"
last_page=$(($pages-1))

for i in $(seq 1 $last_page); do 
  echo "### Page $i/$last_page"; 
  URL="$NXURL/api/v1/path/default-domain/@search?query=$QUERY&pageSize=$BATCH_SIZE&currentPageIndex=$i&maxResults=-1"
   $CURL -o doc$i $URL 2>&1 | grep NX
done

echo "### Dump Nuxeo documents done" `date`
echo "### Total number of docs"
echo $((`cat * | wc -l`/2))
cd .. || exit -3
DIR=`basename $TMPDIR`
echo "### Creating archive..."
time tar czf $DIR.tgz $DIR || exit -4
echo "### Done: " `date`
ls -lh $DIR.tgz
readlink -e $DIR.tgz
#echo "### Cleaning ..."
rm -rf $TMPDIR

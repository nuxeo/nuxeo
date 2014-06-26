#!/bin/bash

URL=$1; shift || exit -1

TMPDIR=/tmp
OUT=$TMPDIR/es-conf.json
rm -f $TMPDIR/settings.json $TMPDIR/mapping.json $OUT

echo "### Dump settings"
curl -s -XGET "$URL/_settings?pretty" -o $TMPDIR/settings.json || exit -1
echo "### Dump mapping"
curl -s -XGET "$URL/_mapping?pretty" -o $TMPDIR/mapping.json || exit -1

echo "### Merging conf"
echo "{" > $OUT
head -n -2 $TMPDIR/settings.json  | tail -n +3  >> $OUT
echo "," >> $OUT
head -n -2 $TMPDIR/mapping.json  | tail -n +3  >> $OUT
echo "}" >> $OUT
echo "### Done"
echo $OUT

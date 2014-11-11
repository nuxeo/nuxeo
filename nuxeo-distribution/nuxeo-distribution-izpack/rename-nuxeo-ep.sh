#!/bin/sh
# rename the unzipped parent directory nuxeo-ep-200xxxxx into nuxeo-ep
# this is a workaround as stuff like
# <unzip src="zipfile.zip" dest="c:/tmp">  <patternset>  <include name="/**/e/**/*"/>  </patternset>  <regexpmapper from=".*/e/(.*)" to="e/\1"/> </unzip>
# does not work

src=$1; shift
dest=$1; shift
#echo "mv $src $dest" > /tmp/a
[ -d $dest ] && exit 1
mv $src/* $dest

#!/bin/bash

for file in  config.xml `find . -name build.xml` ; do 
    echo "Processing $file ..."
    tidy -xml -m -i $file 2>/dev/null
    tab2space -lf $file $file
done


echo "Done."
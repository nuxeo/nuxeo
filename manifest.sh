#!/bin/bash

traverse() {

set -x

pushd $1

for dir in *; do 
 
  [ $dir == '*' ] && continue;

  [ -f $dir ] && continue;

  [ ! -d $dir/target ] && traverse $dir && continue


  [ -d $dir/src/main/resources/META-INF ] && hg mv $dir/src/main/resources/META-INF $dir; 
  [ -d $dir/src/main/resources/OSGI-INF ] && hg mv $dir/src/main/resources/OSGI-INF $dir;    

done

popd 

}

traverse $(pwd)
  

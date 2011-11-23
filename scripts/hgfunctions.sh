#!/bin/bash
#
# Convenient functions for use on Nuxeo projects version controlled under Mercurial 
#
# (C) Copyright 2009-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the GNU Lesser General Public License
# (LGPL) version 2.1 which accompanies this distribution, and is available at
# http://www.gnu.org/licenses/lgpl.html
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# Contributors:
#   Julien Carsique
#
# $Id$

# usage: "hgf pull -u", "hgf id"
hgf() {
  for dir in . nuxeo-*; do
    if [ -d "$dir"/.hg ]; then
      echo "[$dir]"
      (cd "$dir" ; hg "$@"; echo )
    fi
  done
}

hgfa() {
   echo .
   hg "$@" ; echo
   ADDONS=$(mvn help:effective-pom -N|grep '<module>' |cut -d ">" -f 2 |cut -d "<" -f 1)
   for dir in $ADDONS; do
     if [ -d "$dir"/.hg ]; then
       echo "[$dir]"
       (cd "$dir" ; hg "$@"; echo )
     fi
   done
}

_hgx_dir() {
  DIR=$1
  VER=$2
  OPT=$3
  [ -d $DIR ] && (echo $PWD/$DIR ; hg -R $DIR $OPT $VER || true) || (echo ignore $DIR); echo
}

# usage: "hgx 5.3 1.6 up -C", "hgx 5.2 1.5 merge"
hgx() {
  NXP=$1
  NXC=$2
  shift 2;
  if [ -d .hg ]; then
    echo $PWD
    hg $@ $NXP; echo
    # NXC
    for dir in nuxeo-common nuxeo-runtime nuxeo-core; do
      _hgx_dir $dir $NXC "$@"
    done
    # NXP
    for dir in nuxeo-theme nuxeo-shell nuxeo-platform nuxeo-services nuxeo-jsf nuxeo-features \
      nuxeo-dm nuxeo-webengine nuxeo-gwt nuxeo-distribution; do
      _hgx_dir $dir $NXP "$@"
    done
  fi
}

# usage: "hgr old_file new_file"
# It'll perform a rename, keeping changes performed on the new file (package change for instance).
hgr() {
  OLD_FILE=$1
  NEW_FILE=$2
  mv $NEW_FILE "$NEW_FILE.new"
  hg revert $OLD_FILE
  hg rename $OLD_FILE $NEW_FILE
  mv "$NEW_FILE.new" $NEW_FILE
}



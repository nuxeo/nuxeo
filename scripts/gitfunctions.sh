#!/bin/bash
#
# Convenient functions for use on Nuxeo projects version controlled under Git.
#
# (C) Copyright 2009-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the GNU Lesser General Public License
# (LGPL) version 2.1 which accompanies this distribution, and is available at
# http://www.gnu.org/licenses/lgpl-2.1.html
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# Contributors:
#   Julien Carsique
#

# usage: "gitf [git instructions]"
gitf() {
  for dir in . *; do
    if [ -d "$dir"/.git ]; then
      echo "[$dir]"
      (cd "$dir" ; git "$@")
      echo
    fi
  done
}

gitfa() {
  if [ -d "addons" ]; then
    from_root=true
    gitf "$@"
    cd addons
  fi
  ADDONS=$(mvn help:effective-pom -N|grep '<module>' |cut -d ">" -f 2 |cut -d "<" -f 1)
  for dir in . $ADDONS; do
    if [ -d "$dir"/.git ]; then
      echo "[$dir]"
      (cd "$dir" ; git "$@")
      echo
    fi
  done
  [ -z $from_root ] || cd ..
}

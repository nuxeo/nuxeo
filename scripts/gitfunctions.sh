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
HERE=$(cd $(dirname $BASH_SOURCE); pwd -P)
. $HERE/nxutils.sh

# usage: "gitf [git instructions]"
gitf() {
  for dir in . *; do
    if [ -e "$dir"/.git ]; then
      echo "[$dir]"
      (cd "$dir" ; git "$@")
      echo
    fi
  done
}

gitfa() {
  git_args=$@
  function git_command {
    if [ -e .git ]; then
      echo "[$1]"
      git $git_args
      echo
    fi
  }
  _execute_on_modules "git_command"
}

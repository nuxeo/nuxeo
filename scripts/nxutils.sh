#!/bin/bash
#
# Convenient functions for use on Nuxeo projects.
#
# (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Contributors:
#   Kevin Leturc
#

function _retrieve_modules {
  echo $(mvn help:effective-pom -N|grep '<module>' |cut -d ">" -f 2 |cut -d "<" -f 1|sort|uniq|tr '\n' ' ')
}

function _execute_on_modules {
  # Parameters
  local f=$1

  # Detect whether or not we are at Nuxeo repository's root
  if [ -d "addons" ]; then
    from_root=true
  else
    from_root=false
  fi

  # Execute on current directory
  $f "."
  # Loop on addons
  addons="addons addons-core"
  for dir in ${addons}; do
    (
    cd $dir
    $f $dir
    # Loop on second level
    if [ "$from_root" = "true" ]; then
      for sub_dir in $(_retrieve_modules); do
        (cd $sub_dir; $f $sub_dir)
      done
    fi
    )
  done
}

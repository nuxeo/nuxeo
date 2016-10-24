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
#   Kevin Leturc, Julien Carsique
#

# Usage: "_retrieve_modules <result>"
#   <result>: name of the variable which will receive the list of Maven modules of the current POM
function _retrieve_modules {
  local __resultvar=$1
  local _list=LIST_$(echo $(basename $PWD)|tr '-' '_'|tr '[:lower:]' '[:upper:]')
  if [ -z "${!_list}" ]; then
    [ "$quiet" != true ] && echo "Modules list calculated from POM"
    local _result="$(grep '<module>' pom.xml |cut -d ">" -f 2 |cut -d "<" -f 1|sort|uniq|tr '\n' ' ')"
  else
    [ "$quiet" != true ] && echo "Modules list set from environment variable: $_list"
    local _result="${!_list}"
  fi
  eval "$__resultvar='$_result'"
}

function _execute_on_modules {
  local f=$1
  local _modules=""
  _retrieve_modules _modules
  [ "$quiet" != true ] && echo "Execute on modules: $_modules"
  for dir in $_modules; do
    ( cd $dir || continue
      $f $dir
    )
  done
}

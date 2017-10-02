#!/bin/bash -e
##
## (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
##
## Licensed under the Apache License, Version 2.0 (the "License");
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
##     http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
##
## Contributors:
##     Frantz Fischer
##
## Shell script that upgrades Tomcat 7 in Nuxeo (for 6.0, LTS 2015, LTS 2016, LTS 2017)
## Using explicitely bash to avoid dash which is not POSIX.
##
## Requires the following commands to be available: wget, sed, tar, openssl

readonly TOMCAT_ARCHIVE_URL="https://archive.apache.org/dist/tomcat/tomcat-7"
readonly TOMCAT_LATEST_URL="http://www.apache.org/dist/tomcat/tomcat-7"

usage() {
  echo "Usage:"
  echo -e "\t./$(basename "$0") NUXEO_HOME [TOMCAT_TARGET_VERSION]"
  echo
  echo "Note: If no target version is specified the latest one will be retrieved from the Tomcat site"
  echo
  echo "Examples:"
  echo -e "\t./$(basename "$0") /path/to/nuxeo-cap-7.10-tomcat"
  echo -e "\t./$(basename "$0") /path/to/nuxeo-cap-7.10-tomcat 7.0.76"
}

verifyHash() {
  local hash_filename="$1"
  local hash_algorithm="${hash_filename##*.}" # autodetects the algorithm from the extension
  local filename_tocheck
  local hash_tocheck
  local hash_value

  echo -n -e "\tVerifying ${hash_filename}..."

  # file containing the hash
  filename_tocheck="$(cat "${hash_filename}" | cut -d ' ' -f 2 | sed 's/^[\s*]//;s/\s*$//')" || { echo -e "FAILED"; return 1; }
  hash_tocheck="$(cat "${hash_filename}" | cut -d ' ' -f 1 | sed 's/^[\s*]//;s/\s*$//')" || { echo -e "FAILED"; return 1; }

  # file to be checked
  hash_value="$(openssl "${hash_algorithm}" "${filename_tocheck}" | cut -d ' ' -f 2 | sed 's/^[\s*]//;s/\s*$//')" || { echo -e "FAILED"; return 1; }

  if [ "${hash_value}" = "${hash_tocheck}" ]; then
    echo -e "OK"
    return 0
  else
    echo -e "FAILED"
    return 1
  fi
}

if [ $# -eq 0 ] || [ $# -gt 3 ]; then
  usage
  exit 1
fi

# does the Nuxeo location seem valid?
if [ ! -f "$1/templates/nuxeo.defaults" ]; then
  echo "ERROR: Cannot find nuxeo.defaults file. Please check the Nuxeo location."
  echo
  exit 1
fi
NUXEO_HOME=$1

if [ -z "$2" ]; then
  # autodetects latest version
  TOMCAT_TARGET=$(wget -qO- ${TOMCAT_LATEST_URL} | grep href=\"v | sed 's/.*href="v\(7.0.[0-9]*\).*/\1/g')
  TOMCAT_TARGET=${TOMCAT_TARGET:-7.0.75} # fallback to 7.0.75 if autodetection failed
else
  # check the TOMCAT version exists
  VERSIONS_FOUND=$(wget -qO- ${TOMCAT_ARCHIVE_URL} | grep -c "${2}")
  if [ "$VERSIONS_FOUND" -ne 1 ]; then
    echo -e "Cannot find Tomcat version ${2}"
    echo
    exit 1
  else
    TOMCAT_TARGET=$2
  fi
fi

if ! WORK_FOLDER=$(mktemp -d -q /tmp/"$(basename "$0")".XXXXXX); then
  echo "ERROR: Cannot create temp file, exiting..."
  echo
  exit 1
fi

TOMCAT_SOURCE=$(java -cp "${NUXEO_HOME}/lib/catalina.jar" org.apache.catalina.util.ServerInfo | grep "Server number" | sed 's/.*\(7\.0.[0-9]*\).*/\1/g')
TOMCAT_NUXEO_DEFAULT=$(grep tomcat.version "${NUXEO_HOME}/templates/nuxeo.defaults" | cut -d "=" -f 2)

echo "NUXEO_HOME is ${NUXEO_HOME}"
echo "TEMPORARY WORK FOLDER is ${WORK_FOLDER}"
echo "TOMCAT source version (from libs) is ${TOMCAT_SOURCE}"
echo "TOMCAT source version (from nuxeo.defaults) is ${TOMCAT_NUXEO_DEFAULT}"
if [ "${TOMCAT_SOURCE}" = "${TOMCAT_NUXEO_DEFAULT}" ]; then
  echo "TOMCAT source versions match!"
else
  echo "ERROR: TOMCAT source versions don't match!"
  exit 1
fi
echo "TOMCAT target version is ${TOMCAT_TARGET}"
echo

echo "Retrieving files..."
rm -rf "${WORK_FOLDER}"
mkdir -p "${WORK_FOLDER}"
cd "${WORK_FOLDER}"
wget -q --show-progress "${TOMCAT_ARCHIVE_URL}/v${TOMCAT_TARGET}/bin/apache-tomcat-${TOMCAT_TARGET}.tar.gz"
wget -q --show-progress "${TOMCAT_ARCHIVE_URL}/v${TOMCAT_TARGET}/bin/apache-tomcat-${TOMCAT_TARGET}.tar.gz.md5"
wget -q --show-progress "${TOMCAT_ARCHIVE_URL}/v${TOMCAT_TARGET}/bin/apache-tomcat-${TOMCAT_TARGET}.tar.gz.sha1"
wget -q --show-progress "${TOMCAT_ARCHIVE_URL}/v${TOMCAT_TARGET}/bin/extras/tomcat-juli-adapters.jar"
wget -q --show-progress "${TOMCAT_ARCHIVE_URL}/v${TOMCAT_TARGET}/bin/extras/tomcat-juli-adapters.jar.md5"
wget -q --show-progress "${TOMCAT_ARCHIVE_URL}/v${TOMCAT_TARGET}/bin/extras/tomcat-juli-adapters.jar.sha1"
wget -q --show-progress "${TOMCAT_ARCHIVE_URL}/v${TOMCAT_TARGET}/bin/extras/tomcat-juli.jar"
wget -q --show-progress "${TOMCAT_ARCHIVE_URL}/v${TOMCAT_TARGET}/bin/extras/tomcat-juli.jar.md5"
wget -q --show-progress "${TOMCAT_ARCHIVE_URL}/v${TOMCAT_TARGET}/bin/extras/tomcat-juli.jar.sha1"

echo
echo "Checking archives..."
verifyHash "apache-tomcat-${TOMCAT_TARGET}.tar.gz.md5"
verifyHash "apache-tomcat-${TOMCAT_TARGET}.tar.gz.sha1"
verifyHash "tomcat-juli-adapters.jar.md5"
verifyHash "tomcat-juli-adapters.jar.sha1"
verifyHash "tomcat-juli.jar.md5"
verifyHash "tomcat-juli.jar.sha1"

echo
echo "Patching Nuxeo..."
# upgrading files from core distribution
echo -e "\tUncompressing apache archive..."
tar zxf "apache-tomcat-${TOMCAT_TARGET}.tar.gz"
echo -e "\tCopying apache files..."
cp "apache-tomcat-${TOMCAT_TARGET}"/lib/* "${NUXEO_HOME}/lib"
cp "apache-tomcat-${TOMCAT_TARGET}"/bin/*.jar "${NUXEO_HOME}/bin"
cp "apache-tomcat-${TOMCAT_TARGET}/bin/catalina-tasks.xml" "${NUXEO_HOME}/bin"

rm "${NUXEO_HOME}/nxserver/lib/tomcat-jdbc-${TOMCAT_SOURCE}.jar"
cp "apache-tomcat-${TOMCAT_TARGET}/lib/tomcat-jdbc.jar" "${NUXEO_HOME}/nxserver/lib/tomcat-jdbc-${TOMCAT_TARGET}.jar"

rm "${NUXEO_HOME}/nxserver/lib/tomcat-juli-${TOMCAT_SOURCE}.jar"
cp "apache-tomcat-${TOMCAT_TARGET}/bin/tomcat-juli.jar" "${NUXEO_HOME}/nxserver/lib/tomcat-juli-${TOMCAT_TARGET}.jar"

# upgrading files from extras
echo -e "\tCopying apache extras files..."
cp tomcat-juli.jar "${NUXEO_HOME}/bin"
cp tomcat-juli-adapters.jar "${NUXEO_HOME}/lib"

# release files
echo -e "\tCopying apache release files..."
cp "apache-tomcat-${TOMCAT_TARGET}/RELEASE-NOTES" "${NUXEO_HOME}/doc-tomcat"
cp "apache-tomcat-${TOMCAT_TARGET}/LICENSE" "${NUXEO_HOME}/doc-tomcat"
cp "apache-tomcat-${TOMCAT_TARGET}/NOTICE" "${NUXEO_HOME}/doc-tomcat"
cp "apache-tomcat-${TOMCAT_TARGET}/RUNNING.txt" "${NUXEO_HOME}/doc-tomcat"

# nuxeo version bump
echo -e "\tUpdating Tomcat version in nuxeo.defaults..."
sed 's/'"${TOMCAT_SOURCE//./\\.}"'/'"${TOMCAT_TARGET}"'/g' "${NUXEO_HOME}/templates/nuxeo.defaults" > "${WORK_FOLDER}/nuxeo.defaults"
mv "${WORK_FOLDER}/nuxeo.defaults" "${NUXEO_HOME}/templates/nuxeo.defaults"

echo
echo "Deleting temporary folder..."
echo
rm -rf "${WORK_FOLDER}"

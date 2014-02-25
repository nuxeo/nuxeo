#!/bin/bash
#
# Bash command executed by Jenkins to pass parameters and call release
# scripts.
#
# (C) Copyright 2009-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
#

export MAVEN_OPTS="-Xmx1g -Xms1g -XX:MaxPermSize=512m"
echo JAVA_OPTS: $JAVA_OPTS

if [ ! -z $JDK_PATH ]; then
  export JAVA_HOME=$JDK_PATH
  export PATH=$JDK_PATH/bin:$PATH
fi

export PATH=$MAVEN_PATH/bin:$PATH

rm -rf $WORKSPACE/archives/

for file in release.py nxutils.py terminalsize.py IndentedHelpFormatterWithNL.py jenkins_perform.sh; do
  wget --no-check-certificate https://raw.github.com/nuxeo/nuxeo/feature-NXP-13826-release-scripts/scripts/$file -O $file
done

chmod +x *.py
chmod +x *.sh

OPTIONS=( )
if [ $NO_STAGGING = true ]; then
  OPTIONS+=("-d")
fi
if [ $FINAL = true ]; then
  OPTIONS+=("-f")
fi
if [ ! -z $OTHER_VERSION_TO_REPLACE ]; then
  OPTIONS+=("--arv=$OTHER_VERSION_TO_REPLACE")
fi
if [ $SKIP_TESTS = true ]; then
  OPTIONS+=("--skipTests")
fi
if [ ! -z $PROFILES ]; then
  OPTIONS+=("-p $PROFILES")
fi
if [ ! -z "$MSG_COMMIT" ]; then
  #OPTIONS+=("--mc="$(printf %q "$MSG_COMMIT"))
  # FIXME: this will fail if message contains a quote
  OPTIONS+=("--mc=$MSG_COMMIT")
fi
if [ ! -z "$MSG_TAG" ]; then
  #OPTIONS+=("--mt="$(printf %q "$MSG_TAG"))
  # FIXME: this will fail if message contains a quote
  OPTIONS+=("--mt=$MSG_TAG")
fi

echo Prepare release
echo "./release.py prepare -b $BRANCH -t $TAG -n $NEXT_SNAPSHOT -m $MAINTENANCE ${OPTIONS[@]}"
./release.py prepare -b "$BRANCH" -t "$TAG" -n "$NEXT_SNAPSHOT" -m "$MAINTENANCE" "${OPTIONS[@]}" || exit 1

# . $WORKSPACE/release.log
echo Check prepared release
git checkout $BRANCH || exit 1
git pull || exit 1
git push -n origin $BRANCH || exit 1
git log $BRANCH..origin/$BRANCH || exit 1
echo

if [ $NO_STAGGING = true ]; then
  ./release.py perform || exit 1
fi
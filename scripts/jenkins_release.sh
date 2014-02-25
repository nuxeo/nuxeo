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

export MAVEN_OPTS="-Xmx4g -Xms1g -XX:MaxPermSize=512m"
echo JAVA_OPTS: $JAVA_OPTS

if [ ! -z $JDK_PATH ]; then
  export JAVA_HOME=$JDK_PATH
  export PATH=$JDK_PATH/bin:$PATH
fi

export PATH=/opt/apache-maven-2.2.1/bin:$PATH
rm -rf $WORKSPACE/archives/

for file in release.py nxutils.py terminalsize.py IndentedHelpFormatterWithNL.py ; do
  wget --no-check-certificate https://raw.github.com/nuxeo/nuxeo/master/scripts/$file -O $file
done

chmod +x *.py

cd addon
OPTIONS=
if [ $NO_STAGGING != true ]; then
  OPTIONS=-d
fi
if [ $FINAL = true ]; then
  OPTIONS="$OPTIONS -f"
fi
if [ ! -z $OTHER_VERSION_TO_REPLACE ]; then
  OPTIONS="$OPTIONS --arv=$OTHER_VERSION_TO_REPLACE"
fi
if [ $SKIP_TESTS = true ]; then
  OPTIONS="$OPTIONS --skipTests"
fi
if [ ! -z $PROFILES ]; then
  OPTIONS="$OPTIONS -p $PROFILES"
fi
if [ ! -z $MSG_COMMIT ]; then
  OPTIONS="$OPTIONS --mc='$MSG_COMMIT'"
fi
if [ ! -z $MSG_TAG ]; then
  OPTIONS="$OPTIONS --mt='$MSG_TAG'"
fi

../release.py prepare -b $BRANCH -t $TAG -n $NEXT_SNAPSHOT -m $MAINTENANCE $OPTIONS

# . $WORKSPACE/release.log
echo Check prepared release
git checkout $BRANCH
git pull
git push -n origin $BRANCH
git log $BRANCH..origin/$BRANCH
echo

if [ $NO_STAGGING = true ]; then
  ../release.py perform
fi
#!/bin/sh -x
HERE=$(cd $(dirname $0); pwd -P)

# Load conf file if any
if [ -r $HERE/run.conf ]; then
    . $HERE/run.conf
fi

# Default values
HIDE_FF=${HIDE_FF:-}
SUITES=${SUITES:-"suite"}
URL=${URL:-http://localhost:8080/nuxeo/}
FIREFOX_CONF="*firefox"
USER_EXTENSIONS="$HERE/user-extensions.js"

# Build command line
CMD="java -jar selenium-server.jar -log log.txt -port 14440 -timeout 7200 \
      -htmlSuite "*chrome" $URL "
if [ ! -z $HIDE_FF ]; then
    CMD="$CMD"
fi
CMD_END="-firefoxProfileTemplate ffprofile -userExtensions user-extensions.js"

# Clean old results
rm -rf $HERE/result-*.html

cd $HERE
# Update path in user-extensions.js
USER_EXTENSIONS_REPLACED=false
if [ ! -r $USER_EXTENSIONS ]
then
  echo "replacing folder path in $USER_EXTENSIONS file"
  USER_EXTENSIONS_REPLACED=true
  sed "s,/path/to/project-ear/ftest/selenium,$PWD,g" < $USER_EXTENSIONS.sample > $USER_EXTENSIONS
fi

# Update url in profile
sed "s,\(capability.principal.codebase.p0.id...\).*$,\1\"$URL\");,g" < ffprofile/prefs.js.sample > ffprofile/prefs.js

exit_code=0
# Launch suites
for suite in $SUITES; do    
    echo "### [INFO] Running test suite $suite ..."
    $CMD "$PWD/tests/$suite.html" "$PWD/result-$suite.html" $CMD_END
    if [ $? != 0 ]; then
        echo "### [ERROR] $suite TEST FAILURE"
        exit_code=9
    else
        echo "### [INFO] $suite TEST SUCCESSFUL"
    fi
    # pause to prevent "Xvfb failed to start"
    sleep 5
done

if [ $exit_code != 0 ]; then
    echo "### [ERROR] TESTS FAILURE"
else
    echo "### [INFO] TESTS SUCCESSFUL"
fi

exit $exit_code

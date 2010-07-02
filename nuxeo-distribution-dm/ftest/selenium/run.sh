#!/bin/sh -x
HERE=$(cd $(dirname $0); pwd -P)

# Load conf file if any
if [ -r $HERE/run.conf ]; then
    . $HERE/run.conf
fi

# Default values
HIDE_FF=${HIDE_FF:-}
SUITES=${SUITES:-"suite1 suite2 suite-dm suite-webengine suite-webengine-website suite-webengine-tags"}
URL=${URL:-http://localhost:8080/nuxeo/}

# Download selenium-server.jar
(cd $HERE; mvn dependency:copy) || exit 1

# Build command line
CMD="java -jar selenium-server.jar -port 14440 -timeout 7200 \
      -htmlSuite "*chrome" $URL "
if [ ! -z $HIDE_FF ]; then
    # we assume that there is a long running xvfb-run process running and
    # listening on display :1
    export DISPLAY=${DISPLAY:-":1"}
#    CMD="xvfb-run -a $CMD"
fi
CMD_END="-firefoxProfileTemplate ffprofile -userExtensions user-extensions.js"

# Clean old results
rm -rf $HERE/result-*.html

cd $HERE
# Update path in user-extensions.js
sed "s,/path/to/project-ear/ftest/selenium,$PWD,g" < user-extensions.js.sample > user-extensions.js

# Update firefox profile setting target url permission
sed "s,\(capability.principal.codebase.p0.id...\).*$,\1\"$URL\");,g" < ffprofile/prefs.js.sample > ffprofile/prefs.js

if [ ! -z $HIDE_FF ]; then
    # Speed up firefox when hidden
    echo '// do not load images' >> ffprofile/prefs.js
    echo 'user_pref("permissions.default.image", 2);' >> ffprofile/prefs.js
fi

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

#!/bin/sh -x
HERE=$(cd $(dirname $0); pwd -P)

# Load conf file if any
if [ -r $HERE/run.conf ]; then
    . $HERE/run.conf
fi

# Default values
HIDE_FF=${HIDE_FF:-}
SUITES=${SUITES:-"suite1 suite2"}
URL=${URL:-http://localhost:8080/nuxeo/}

# Build command line
CMD="java -jar selenium-server.jar -port 14440 -timeout 7200 \
      -htmlSuite "*chrome" $URL "
if [ ! -z $HIDE_FF ]; then
    CMD="xvfb-run $CMD"
fi
CMD_END=" -userExtensions user-extensions.js"

# Clean old results
rm -rf $HERE/result-*.html

# Launch suites
cd $HERE
for suite in $SUITES; do
    echo "### Running test suite $suite ..."
    $CMD "$PWD/tests/$suite.html" "$PWD/result-$suite.html" $CMD_END || exit 1
    # pause to prevent "Xvfb failed to start"
    sleep 5
done

echo "### Successful"

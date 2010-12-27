NUXEO_HOME=${NUXEO_HOME:-$(cd $(dirname $0)/../nuxeo-distribution-tomcat/target/nuxeo-dm-5.4.1-SNAPSHOT-tomcat; pwd -P)}
PARAM_NUXEO_HOME="-Dnuxeo.home=$NUXEO_HOME/"
NUXEO_CONF=${NUXEO_CONF:-$NUXEO_HOME/bin/nuxeo.conf}
PARAM_NUXEO_CONF="-Dnuxeo.conf=$NUXEO_CONF"

echo java "$PARAM_NUXEO_HOME" "$PARAM_NUXEO_CONF" -jar target/nuxeo-launcher-5.4.1-SNAPSHOT-jar-with-dependencies.jar $@
java "$PARAM_NUXEO_HOME" "$PARAM_NUXEO_CONF" -jar target/nuxeo-launcher-5.4.1-SNAPSHOT-jar-with-dependencies.jar $@

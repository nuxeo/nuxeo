JAVA_OPTS="-Xms512m -Xmx1024m -XX:MaxPermSize=512m -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 -Dfile.encoding=UTF-8"

NUXEO_HOME=${NUXEO_HOME:-$(cd $(dirname $0)/../nuxeo-distribution-tomcat/target/nuxeo-dm-5.4.1-SNAPSHOT-tomcat; pwd -P)}
PARAM_NUXEO_HOME="-Dnuxeo.home=$NUXEO_HOME/"
NUXEO_CONF=${NUXEO_CONF:-$NUXEO_HOME/bin/nuxeo.conf}
PARAM_NUXEO_CONF="-Dnuxeo.conf=$NUXEO_CONF"

echo java $JAVA_OPTS "$PARAM_NUXEO_HOME" "$PARAM_NUXEO_CONF" -jar target/nuxeo-launcher-5.4.1-SNAPSHOT-jar-with-dependencies.jar $@
java $JAVA_OPTS "$PARAM_NUXEO_HOME" "$PARAM_NUXEO_CONF" -jar target/nuxeo-launcher-5.4.1-SNAPSHOT-jar-with-dependencies.jar $@

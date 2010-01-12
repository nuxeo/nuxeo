# shell script to run selenium tests on hudson

HERE=$(cd $(dirname $0); pwd -P)
NXDIST="$HERE/nuxeo-distribution"
NXVERSION=${NXVERSION:-5.3}
JBOSS_HOME="$NXDIST/nuxeo-distribution-jboss/target/nuxeo-ep-jboss"

echo "get nuxeo distribution"
if [ ! -d $NXDIST ]; then
  hg clone -r $NXVERSION http://hg.nuxeo.org/nuxeo/nuxeo-distribution $NXDIST 2>/dev/null || exit 1
else
  (cd $NXDIST && hg pull && hg up $NXVERSION) || exit 1
fi

echo "deploy nuxeo distribution"
mvn clean package -Pjboss,nuxeo-ep -f $NXDIST/pom.xml || exit 1

echo  "deploy plugin"
ant deploy -Djboss.dir=$JBOSS_HOME || exit 1

echo "cleanup jboss"
rm -rf "$JBOSS_HOME"/server/default/data/*
rm -rf "$JBOSS_HOME"/server/default/log/*

echo "start jboss"
chmod +x $JBOSS_HOME/bin/jbossctl || exit 1
$JBOSS_HOME/bin/jbossctl start || exit 1

echo "run selenium tests"
chmod +x "$HERE"/run.sh
HIDE_FF=true "$HERE"/run.sh
ret1=$?

echo "stop jboss"
$JBOSS_HOME/bin/jbossctl stop || exit 1

exit $ret1

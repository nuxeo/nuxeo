#!/bin/sh
#MVN_OPTS=-o
ASSEMBLY=$1
shift;

case "$ASSEMBLY" in
    nuxeo)
        echo "building standard nuxeo.ear"
        mvn $MVN_OPTS package $@
  ;;
    nuxeo-2parts)
        echo "building 2 JVMs nuxeo distrib"
        mvn -Dnuxeo.ear.assembly=nuxeo-platform-stateful $MVN_OPTS install package $@
        mvn -Dnuxeo.ear.assembly=nuxeo-web-stateless $MVN_OPTS package -N $@
  ;;
    help)
        echo "Usage: $0 {help|<target config>} [-Pmaven_profile]"
        echo "target config can be :"
        echo "nuxeo : building standard nuxeo.ear"
        echo "nuxeo-2parts : building 2 ears for stateful (platform) and stateless parts (web)"
        ;;
    *)
        echo "Usage: $0 {help|nuxeo|nuxeo-2parts} [-Pmaven_profile]"
        exit 1
        ;;
esac

exit 0


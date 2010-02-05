#!/bin/sh
#MVN_OPTS=-o
ASSEMBLY=$1
shift;

case "$ASSEMBLY" in
    help)
        echo "Usage: $0 [help] [-Pmaven_profile]"
        echo "maven_profile can be : derby, mysql, postgresql, oracle, h2, jcr, jcr-pgsql"
        exit 1
        ;;
    nuxeo-2parts)
        echo "building nuxeo two parts (stateless/stateful)"
        mvn $MVN_OPTS clean package -Pnuxeo-2parts
        ;;
    *)
        echo "building standard nuxeo.ear"
        mvn $MVN_OPTS package $@
        ;;
esac

exit 0


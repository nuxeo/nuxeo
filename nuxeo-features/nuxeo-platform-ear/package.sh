#!/bin/sh
MVN_OPTS=-o

case "$1" in
    nuxeo)
        echo "building standard nuxeo.ear"
  mvn $MVN_OPTS package
  ;;
    nuxeo-core)
        echo "building ear for nuxeo core server"
        mvn -Dnuxeo.ear.assembly=$1 $MVN_OPTS package
  ;;
    nuxeo-indexing)
        echo "building ear for nuxeo indexing server"
        mvn -Dnuxeo.ear.assembly=$1 $MVN_OPTS package
  ;;
    nuxeo-core-sync-index)
        echo "building ear for nuxeo core and sync indexer"
        mvn -Dnuxeo.ear.assembly=$1 $MVN_OPTS package
  ;;
    nuxeo-webplatform)
        echo "building ear for webplatform (everything except core and indexing)"
        mvn -Dnuxeo.ear.assembly=$1 $MVN_OPTS package
  ;;
    nuxeo-simplewebapp)
        echo "building complete ear without unecessary facades"
        mvn -Dnuxeo.ear.assembly=$1 $MVN_OPTS package
  ;;
    nuxeo-3parts)
        echo "building 3 JVMs nuxeo distrib"
        mvn -Dnuxeo.ear.assembly=nuxeo-core $MVN_OPTS package
        mvn -Dnuxeo.ear.assembly=nuxeo-indexing $MVN_OPTS package -N
        mvn -Dnuxeo.ear.assembly=nuxeo-webplatform $MVN_OPTS package -N
  ;;
    old-nuxeo-3parts)
        echo "building 3 JVMs nuxeo distrib"
        mvn -Dnuxeo.ear.assembly=old-nuxeo-core $MVN_OPTS package
        mvn -Dnuxeo.ear.assembly=old-nuxeo-indexing $MVN_OPTS package -N
        mvn -Dnuxeo.ear.assembly=old-nuxeo-webplatform $MVN_OPTS package -N
  ;;

    nuxeo-2parts)
        echo "building 2 JVMs nuxeo distrib"
        mvn -Dnuxeo.ear.assembly=nuxeo-platform-stateful $MVN_OPTS package
        mvn -Dnuxeo.ear.assembly=nuxeo-web-stateless $MVN_OPTS package -N
  ;;
    old-nuxeo-2parts)
        echo "building 2 JVMs nuxeo distrib"
        mvn -Dnuxeo.ear.assembly=old-nuxeo-platform-stateful $MVN_OPTS package
        mvn -Dnuxeo.ear.assembly=old-nuxeo-web-stateless $MVN_OPTS package -N
  ;;

    help)
        echo "Usage: $0 {help|<target config>}"
        echo "target config can be :"
        echo "nuxeo : building standard nuxeo.ear"
        echo "nuxeo-core : building ear for nuxeo core server"
        echo "nuxeo-indexing : building ear for nuxeo indexing server"
        echo "nuxeo-core-sync-index  : building ear for nuxeo core and a synchronous indexing server (require SQL storage for Compass)"
        echo "nuxeo-webplatform : building ear for webplatform (everything except core and indexing)"
        echo "nuxeo-simplewebapp : building complete ear without unecessay facades"
        echo "nuxeo-3parts : building 3 ears for core/indexing/webplatform"
        echo "nuxeo-2parts : building 2 ears for stateful (platform) and stateless parts (web)"
        ;;
    *)
        echo "Usage: $0 {help|nuxeo|nuxeo-simplewebapp|nuxeo-core|nuxeo-indexing|nuxeo-webplatform|nuxeo-3parts|nuxeo-2parts}"
        exit 1
        ;;
esac

exit 0


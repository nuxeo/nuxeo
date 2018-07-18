#!/bin/bash -ex

[ -f /etc/kafka.properties ] && exit 0

cat <<! > /etc/kafka.properties
kafka.bootstrap.servers=${KAFKA_ADVERTISED_HOST_NAME:-localhost}:${KAFKA_ADVERTISED_PORT:-9092}
!

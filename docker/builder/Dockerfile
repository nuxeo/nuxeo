# Nuxeo server builder image
#
# It provides:
# - A Nuxeo server distribution with appropriate permissions in the /distrib directory.
# - An install-packages.sh script to install Nuxeo packages.
#
# It is based on an OpenJDK image as it requires Java for the package installation.
#
# It must be used within a multi-stage build.
# For instance, you can use the following Dockerfile sample to build an image containing a Nuxeo server distribution:
# - In the NUXEO_HOME directory.
# - With some Nuxeo packages installed.
# - Owned by the UID user and GID group.
#
# FROM nuxeo/builder:VERSION as builder
# COPY local/packages /packages
# RUN install-packages.sh /packages
#
# FROM BASE_IMAGE
# COPY --from=builder --chown=UID:GID /distrib NUXEO_HOME

FROM openjdk:11-jdk-slim
LABEL maintainer="Nuxeo <packagers@nuxeo.com>"

RUN apt-get update && apt-get install -y --no-install-recommends \
  procps \
  unzip \
  && rm -rf /var/lib/apt/lists/*

COPY target/distrib/nuxeo-server-tomcat-*.zip /tmp/nuxeo-distribution-tomcat.zip

ENV NUXEO_HOME=/distrib

RUN mkdir -p /tmp/nuxeo-distribution \
  && unzip -q -d /tmp/nuxeo-distribution /tmp/nuxeo-distribution-tomcat.zip \
  && DISTDIR=$(/bin/ls /tmp/nuxeo-distribution | head -n 1) \
  && mv /tmp/nuxeo-distribution/$DISTDIR $NUXEO_HOME \
  && sed -i -e "s/^org.nuxeo.distribution.package.*/org.nuxeo.distribution.package=docker/" $NUXEO_HOME/templates/common/config/distribution.properties \
  && rm -rf /tmp/nuxeo-distribution* \
  && chmod +x $NUXEO_HOME/bin/*ctl $NUXEO_HOME/bin/*.sh \
  && chmod -R g+rwX $NUXEO_HOME

ENV NUXEO_CONF=$NUXEO_HOME/bin/nuxeo.conf

COPY install-packages.sh /usr/local/bin

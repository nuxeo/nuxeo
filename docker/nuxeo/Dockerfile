# Nuxeo server image
#
# It includes a bare Nuxeo server without any Nuxeo package installed and some basic Open Source converters.
#
# It uses a multi-stage build to copy and unzip the Nuxeo server ZIP file.

ARG BASE_IMAGE
# ------------------------------------------------------------------------
# Build stage
FROM azul/zulu-openjdk:17 as builder

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
  # Make sure the packages/* directories belong to nuxeo, to avoid a permission issue with nuxeoctl mp-install
  # in case they are mounted as a volume (then owned by root:root)
  && mkdir -p $NUXEO_HOME/packages/backup \
  && mkdir -p $NUXEO_HOME/packages/store \
  && mkdir -p $NUXEO_HOME/packages/tmp \
  && rm -rf /tmp/nuxeo-distribution* \
  && chmod +x $NUXEO_HOME/bin/*ctl $NUXEO_HOME/bin/*.sh \
  && chmod -R g+rwX $NUXEO_HOME

# ------------------------------------------------------------------------
# Target stage
FROM $BASE_IMAGE

ARG BUILD_TAG
ARG SCM_REF
ARG VERSION

ARG LIBREOFFICE_VERSION=7.1.1

LABEL org.nuxeo.build-tag=$BUILD_TAG
LABEL org.nuxeo.scm-ref=$SCM_REF
LABEL org.nuxeo.version=$VERSION
# Override parent ones
LABEL org.label-schema.name="Nuxeo Server"
LABEL org.opencontainers.image.title="Nuxeo Server"

# Add a nuxeo user with a fixed UID
# We chose an arbitrary UID that doesn't conflict with possibly existing users
ENV NUXEO_USER nuxeo
RUN useradd -m -d /home/$NUXEO_USER -u 900 -s /bin/bash $NUXEO_USER

ENV NUXEO_HOME /opt/nuxeo/server
ENV NUXEO_CONF /etc/nuxeo/nuxeo.conf

# Set permissions on writeable directories to support arbitrary user IDs for OpenShift.
# These directories must be owned by the root group and be readable/writable by that group.
# See https://docs.openshift.com/container-platform/3.5/creating_images/guidelines.html#use-uid
RUN mkdir -p $NUXEO_HOME \
  # Create folders until `nuxeo.war` to allow it to be mounted
  && mkdir -p $NUXEO_HOME/nxserver/nuxeo.war \
  && chown 900:0 $NUXEO_HOME && chmod g+rwX $NUXEO_HOME \
  && mkdir -p /etc/nuxeo \
  && chown 900:0 /etc/nuxeo && chmod g+rwX /etc/nuxeo \
  && mkdir -p /var/lib/nuxeo \
  && chown 900:0 /var/lib/nuxeo && chmod g+rwX /var/lib/nuxeo \
  && mkdir -p /var/log/nuxeo \
  && chown  900:0 /var/log/nuxeo && chmod g+rwX /var/log/nuxeo \
  && mkdir -p /var/pid/nuxeo \
  && chown 900:0 /var/pid/nuxeo && chmod g+rwX /var/pid/nuxeo \
  && chmod g=u /etc/passwd

COPY docker-entrypoint.sh /

# Copy packages installation script
COPY install-packages.sh /

# Copy script to run Nuxeo in dev environment
COPY nuxeo-run-dev.sh /

# Create directory in which to mount property files appended to nuxeo.conf at runtime
RUN mkdir /etc/nuxeo/conf.d
# Copy base property files.
COPY conf.d /etc/nuxeo/conf.d/

# Create directory in which to copy shell scripts to run at runtime
RUN mkdir /docker-entrypoint-initnuxeo.d && chmod g+rwx,o+rx /docker-entrypoint-initnuxeo.d \
  && chmod -R g+rwX,o+rX /etc/nuxeo/conf.d \
  && chmod g+rwx,o+rx /docker-entrypoint.sh \
  && chmod g+rwx,o+rx /install-packages.sh

# Copy Nuxeo distribution
COPY --from=builder /distrib $NUXEO_HOME

# Work around missing support for --chown flag with COPY instruction in Kaniko
# TODO NXP-28052: remove and use COPY --chown when fixed in Kaniko, or find a proper way
RUN chown -R 900:0 $NUXEO_HOME \
  && chmod -R g+rwX $NUXEO_HOME

VOLUME /var/lib/nuxeo
VOLUME /var/log/nuxeo
VOLUME /tmp

ENV PATH $NUXEO_HOME/bin:$PATH

EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nuxeoctl", "console"]

# Run as a non root user with a fixed UID
USER 900

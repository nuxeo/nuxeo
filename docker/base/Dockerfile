# Nuxeo server base image
#
# Based on CentOS 7, it includes:
# - OpenJDK.
# - A nuxeo user with the 1000 fixed UID.
# - The directories required to have the configuration, data and logs outside of the server directory, with appropriate permissions.
# - An entrypoint script to configure the server.
# - The default recommended volumes.
# - The environment variables required by the server.
#
# It doesn't contain the Nuxeo server distribution itself.
# To build an image containing a Nuxeo server distribution, you should use a multi-stage build
# with the nuxeo/builder image and the current image as a base image, as in the following sample Dockerfile:
#
# FROM nuxeo/builder:VERSION as builder
# COPY local/packages /packages
# RUN install-packages.sh /packages
#
# FROM nuxeo/base:VERSION
# RUN yum -y install ...
# COPY --from=builder --chown=1000:0 /distrib $NUXEO_HOME
# USER 1000

FROM centos:7
LABEL maintainer="Nuxeo <packagers@nuxeo.com>"

RUN yum -y update && yum -y --setopt=skip_missing_names_on_install=False install \
  java-11-openjdk java-11-openjdk-devel \
  less \
  tar \
  unzip \
  wget \
  && yum clean all

# Set Java 11 as default java (if libreoffice is installed in the final image, it adds Java 8 and sets it as default)
RUN alternatives --set java java-11-openjdk.x86_64

# Remove setuid/setgid binaries from images for security
RUN find / -perm 6000 -type f -exec chmod a-s {} \; || true

# Add a nuxeo user with a fixed UID
ENV NUXEO_USER nuxeo
RUN useradd -m -d /home/$NUXEO_USER -u 1000 -s /bin/bash $NUXEO_USER

ENV NUXEO_HOME /opt/nuxeo/server

# Set permissions on writeable directories to support arbitrary user IDs for OpenShift.
# These directories must be owned by the root group and be readable/writable by that group.
# See https://docs.openshift.com/container-platform/3.5/creating_images/guidelines.html#use-uid
RUN mkdir -p $NUXEO_HOME \
  && chown 1000:0 $NUXEO_HOME && chmod g+rwX $NUXEO_HOME \
  && mkdir -p /etc/nuxeo \
  && chown 1000:0 /etc/nuxeo && chmod g+rwX /etc/nuxeo \
  && mkdir -p /var/lib/nuxeo/data \
  && chown 1000:0 /var/lib/nuxeo/data && chmod g+rwX /var/lib/nuxeo/data \
  && mkdir -p /var/log/nuxeo \
  && chown  1000:0 /var/log/nuxeo && chmod g+rwX /var/log/nuxeo \
  && mkdir -p /var/pid/nuxeo \
  && chown 1000:0 /var/pid/nuxeo && chmod g+rwX /var/pid/nuxeo \
  && chmod g=u /etc/passwd

COPY docker-entrypoint.sh /

VOLUME /var/lib/nuxeo/data
VOLUME /var/log/nuxeo
VOLUME /tmp

ENV HOME $NUXEO_HOME
ENV PATH $NUXEO_HOME/bin:$PATH
ENV NUXEO_CONF /etc/nuxeo/nuxeo.conf

WORKDIR $NUXEO_HOME
EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nuxeoctl", "console"]

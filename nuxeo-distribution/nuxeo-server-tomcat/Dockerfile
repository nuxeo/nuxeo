# vim:set ft=dockerfile:

# ------------------------------------------------------------------------
# Build Stage: unzip the Nuxeo distribution
FROM alpine:3.10.2 as distrib

COPY target/nuxeo-server-tomcat-*.zip /tmp/nuxeo-distribution-tomcat.zip

RUN mkdir -p /tmp/nuxeo-distribution \
  && unzip -q -d /tmp/nuxeo-distribution /tmp/nuxeo-distribution-tomcat.zip \
  && DISTDIR=$(/bin/ls /tmp/nuxeo-distribution | head -n 1) \
  && mv /tmp/nuxeo-distribution/$DISTDIR /distrib \
  && sed -i -e "s/^org.nuxeo.distribution.package.*/org.nuxeo.distribution.package=docker/" /distrib/templates/common/config/distribution.properties \
  && rm -rf /tmp/nuxeo-distribution* \
  && chmod +x /distrib/bin/*ctl /distrib/bin/*.sh

COPY docker/docker-template /distrib/templates/docker


# ------------------------------------------------------------------------
# Build Stage: base image with Java and converters
FROM centos:7 as base

# Add Nuxeo Jenkins X Yum repository
COPY nuxeo.repo /etc/yum.repos.d/

RUN yum -y update && yum -y install epel-release && yum -y --setopt=skip_missing_names_on_install=False install \
  ccextractor-0.88-1.el7 \
  ffmpeg-4.1.4-4.el7 \
  ghostscript \
  ImageMagick \
  java-11-openjdk java-11-openjdk-devel \
  less \
  libreoffice-headless libreoffice-writer libreoffice-calc libreoffice-impress \
  libwpd-tools \
  # required by perl-Image-ExifTool to extract binary metadata from open office document
  perl-Archive-Zip \
  perl-Image-ExifTool \
  poppler-utils \
  tar \
  ufraw \
  wget \
  && yum clean all

# Set Java 11 as default java (libreoffice adds Java 8)
RUN alternatives --set java java-11-openjdk.x86_64

# Remove setuid/setgid binaries from images for security
RUN find / -perm 6000 -type f -exec chmod a-s {} \; || true


# ------------------------------------------------------------------------
# Target stage: Nuxeo image
FROM base
LABEL maintainer="Nuxeo <packagers@nuxeo.com>"

# Add a nuxeo user
ENV NUXEO_USER nuxeo
RUN useradd -m -d /home/$NUXEO_USER -u 1000 -s /bin/bash $NUXEO_USER

# Copy distribution
ENV NUXEO_HOME /opt/nuxeo/server
COPY --from=distrib /distrib $NUXEO_HOME
# Work around missing support for --chown flag with COPY instruction in Kaniko
# TODO NXP-28052: remove and use COPY --chown when fixed in Kaniko, or find a proper way
RUN chown -R 1000:0 $NUXEO_HOME \
  && chmod -R g+rwX $NUXEO_HOME

COPY --chown=1000:0 docker/nuxeo.conf /etc/nuxeo/nuxeo.conf.template
COPY docker/docker-entrypoint.sh /

# Set permissions on writeable directories to support arbitrary user IDs for OpenShift.
# These directories must be owned by the root group and be read/writable by that group.
# See https://docs.openshift.com/container-platform/3.4/creating_images/guidelines.html
RUN chown -R 1000:0 /etc/nuxeo && chmod g+rwX /etc/nuxeo && rm -f $NUXEO_HOME/bin/nuxeo.conf \
  && mkdir -p /var/lib/nuxeo/data \
  && chown -R 1000:0 /var/lib/nuxeo/data && chmod -R g+rwX /var/lib/nuxeo/data \
  && mkdir -p /var/log/nuxeo \
  && chown -R 1000:0 /var/log/nuxeo && chmod -R g+rwX /var/log/nuxeo \
  && mkdir -p /var/pid/nuxeo \
  && chown -R 1000:0 /var/pid/nuxeo && chmod -R g+rwX /var/pid/nuxeo \
  && mkdir -p /docker-entrypoint-initnuxeo.d \
  && chown -R 1000:0 /docker-entrypoint-initnuxeo.d && chmod -R g+rwX /docker-entrypoint-initnuxeo.d  \
  && chmod g=u /etc/passwd

ENV NUXEO_CONF /etc/nuxeo/nuxeo.conf
ENV HOME $NUXEO_HOME
ENV PATH $NUXEO_HOME/bin:$PATH
WORKDIR $NUXEO_HOME
EXPOSE 8080
USER 1000
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nuxeoctl","console"]

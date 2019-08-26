# vim:set ft=dockerfile:
FROM       centos:7
MAINTAINER Nuxeo <packagers@nuxeo.com>

# Install wget and tar
RUN yum -y install wget tar

# Add Nuxeo Jenkins X Yum repository
COPY nuxeo.repo /etc/yum.repos.d/

# Add needed convert tools
RUN yum -y update && yum -y install epel-release && yum -y --setopt=skip_missing_names_on_install=False install \
  java-11-openjdk java-11-openjdk-devel \
  ccextractor-0.88-1.el7 \
  ffmpeg-4.1.4-4.el7 \
  ghostscript \
  ImageMagick \
  libreoffice \
  libwpd-tools \
  # required by perl-Image-ExifTool to extract binary metadata from open office document
  perl-Archive-Zip \
  perl-Image-ExifTool \
  poppler-utils \
  ufraw \
  && yum clean all

# Set Java 11 as default java (libreoffice adds Java 8)
RUN alternatives --set java java-11-openjdk.x86_64

# Remove setuid/setgid binaries from images for security
RUN find / -perm 6000 -type f -exec chmod a-s {} \; || true

ENV NUXEO_USER nuxeo
ENV NUXEO_HOME /opt/nuxeo/server
ENV HOME /opt/nuxeo/server
ARG NUXEO_VERSION=master

# Create Nuxeo user
RUN useradd -m -d /home/$NUXEO_USER -u 1000 -s /bin/bash $NUXEO_USER

# Add distribution
COPY target/nuxeo-server-tomcat-*.zip /tmp/nuxeo-distribution-tomcat.zip
RUN mkdir -p /tmp/nuxeo-distribution $(dirname $NUXEO_HOME) \
  && unzip -q -d /tmp/nuxeo-distribution /tmp/nuxeo-distribution-tomcat.zip \
  && DISTDIR=$(/bin/ls /tmp/nuxeo-distribution | head -n 1) \
  && mv /tmp/nuxeo-distribution/$DISTDIR $NUXEO_HOME \
  && sed -i -e "s/^org.nuxeo.distribution.package.*/org.nuxeo.distribution.package=docker/" $NUXEO_HOME/templates/common/config/distribution.properties \
  && rm -rf /tmp/nuxeo-distribution* \
  && chmod +x $NUXEO_HOME/bin/*ctl $NUXEO_HOME/bin/*.sh \
  # For arbitrary user in the admin group
  && chmod g+rwX $NUXEO_HOME/bin/*ctl $NUXEO_HOME/bin/*.sh \
  && $NUXEO_HOME/bin/nuxeoctl mp-init \
  && chown -R 1000:0 $NUXEO_HOME && chmod -R g+rwX $NUXEO_HOME

COPY docker/docker-template $NUXEO_HOME/templates/docker
COPY docker/nuxeo.conf /etc/nuxeo/nuxeo.conf.template
ENV NUXEO_CONF /etc/nuxeo/nuxeo.conf

# Protecting writeable directories to support arbitrary User IDs for OpenShift
# https://docs.openshift.com/container-platform/3.4/creating_images/guidelines.html
RUN chown -R 1000:0 /etc/nuxeo && chmod g+rwX /etc/nuxeo && rm -f $NUXEO_HOME/bin/nuxeo.conf \
  && mkdir -p /var/lib/nuxeo/data \
  && chown -R 1000:0 /var/lib/nuxeo/data && chmod -R g+rwX /var/lib/nuxeo/data \
  && mkdir -p /var/log/nuxeo \
  && chown -R 1000:0 /var/log/nuxeo && chmod -R g+rwX /var/log/nuxeo \
  && mkdir -p /var/run/nuxeo \
  && chown -R 1000:0 /var/run/nuxeo && chmod -R g+rwX /var/run/nuxeo \
  && mkdir -p /docker-entrypoint-initnuxeo.d \
  && chown -R 1000:0 /docker-entrypoint-initnuxeo.d && chmod -R g+rwX /docker-entrypoint-initnuxeo.d  \
  && chmod g=u /etc/passwd

ENV PATH $NUXEO_HOME/bin:$PATH

WORKDIR $NUXEO_HOME
COPY docker/docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
EXPOSE 8080
CMD ["nuxeoctl","console"]

USER 1000

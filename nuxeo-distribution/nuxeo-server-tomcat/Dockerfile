# vim:set ft=dockerfile:

# ------------------------------------------------------------------------
# Build Stage: Unzip the distribution
FROM alpine:3.10.2 as distrib

COPY target/nuxeo-server-tomcat-*.zip /tmp/nuxeo-distribution-tomcat.zip

RUN mkdir -p /tmp/nuxeo-distribution \
  && unzip -q -d /tmp/nuxeo-distribution /tmp/nuxeo-distribution-tomcat.zip \
  && DISTDIR=$(/bin/ls /tmp/nuxeo-distribution | head -n 1) \
  && mv /tmp/nuxeo-distribution/$DISTDIR /distrib \
  && sed -i -e "s/^org.nuxeo.distribution.package.*/org.nuxeo.distribution.package=docker/" /distrib/templates/common/config/distribution.properties \
  && rm -rf /tmp/nuxeo-distribution* \
  && chmod +x /distrib/bin/*ctl /distrib/bin/*.sh \
  && chown -R 1000:0 /distrib && chmod -R g+rwX /distrib

COPY docker/docker-template /distrib/templates/docker


# ------------------------------------------------------------------------
# Build Stage: Base image with Java and converters
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
# Target Stage: Nuxeo image
FROM base
LABEL maintainer="Nuxeo <packagers@nuxeo.com>"

# Copy distribution
ENV NUXEO_HOME /opt/nuxeo/server
COPY --from=distrib --chown=1000:0 /distrib $NUXEO_HOME
COPY --chown=1000:0 docker/nuxeo.conf /etc/nuxeo/nuxeo.conf.template
COPY docker/docker-entrypoint.sh /

# Add a nuxeo user
ENV NUXEO_USER nuxeo
RUN useradd -m -d $NUXEO_HOME -u 1000 -s /bin/bash $NUXEO_USER

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

ENV NUXEO_CONF /etc/nuxeo/nuxeo.conf
ENV HOME $NUXEO_HOME
ENV PATH $NUXEO_HOME/bin:$PATH
WORKDIR $NUXEO_HOME
EXPOSE 8080
USER 1000
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nuxeoctl","console"]

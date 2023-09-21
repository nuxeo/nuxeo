# Nuxeo server image
#
# It includes a bare Nuxeo server without any Nuxeo package installed and some basic Open Source converters.
#
# It uses a multi-stage build to copy and unzip the Nuxeo server ZIP file.

# ------------------------------------------------------------------------
# Build stage
FROM azul/zulu-openjdk:11 as builder

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
FROM centos:7

ARG BUILD_TAG
ARG SCM_REF
ARG VERSION

ARG LIBREOFFICE_VERSION=7.4.2

LABEL org.nuxeo.build-tag=$BUILD_TAG
LABEL org.nuxeo.scm-ref=$SCM_REF
LABEL org.nuxeo.version=$VERSION
# Override parent ones
LABEL org.label-schema.build-date=""
LABEL org.label-schema.license="Apache 2.0"
LABEL org.label-schema.name="Nuxeo Server"
LABEL org.label-schema.vendor="Nuxeo"
LABEL org.opencontainers.image.created=""
LABEL org.opencontainers.image.licenses="Apache 2.0"
LABEL org.opencontainers.image.title="Nuxeo Server"
LABEL org.opencontainers.image.vendor="Nuxeo"

# Configure Zulu Repository
RUN rpm --import http://repos.azulsystems.com/RPM-GPG-KEY-azulsystems \
  && rpm --install https://cdn.azul.com/zulu/bin/zulu-repo-1.0.0-1.noarch.rpm

# Install required software
RUN yum -y update \
  && yum -y --setopt=skip_missing_names_on_install=False install \
    # required for python27-python
    centos-release-scl-rh \
    epel-release \
    # Install java first to provide it for depend packages (such as libreoffice)
    zulu11-jdk \
  # Install LibreOffice
  && curl -f -L https://packages.nuxeo.com/repository/document-foundation-raw/LibreOffice_${LIBREOFFICE_VERSION}_Linux_x86-64_rpm.tar.gz | tar -C /tmp -xzv \
  && yum -y localinstall /tmp/LibreOffice_${LIBREOFFICE_VERSION}*/RPMS/*.rpm \
  && ln -s /opt/libreoffice$(echo $LIBREOFFICE_VERSION | cut -f 1,2 -d ".")/program/soffice /usr/bin/soffice \
  && ln -s /opt/libreoffice$(echo $LIBREOFFICE_VERSION | cut -f 1,2 -d ".")/program/soffice /usr/bin/libreoffice \
  && rm -rf /tmp/LibreOffice_${LIBREOFFICE_VERSION}* \
  # latest ImageMagick6 update for CentOS 7 is provided by Remi's RPM repository
  && yum -y install https://rpms.remirepo.net/enterprise/remi-release-7.rpm \
  && yum -y --enablerepo=remi install ImageMagick6 \
  # install other tools
  && yum -y --setopt=skip_missing_names_on_install=False install \
    ghostscript \
    less \
    libwpd-tools \
    # required by exiftool to extract binary metadata from open office document
    perl-Archive-Zip \
    perl-Image-ExifTool \
    poppler-utils \
    # install Python 2.7.18
    python27-python \
    tar \
    ufraw \
    unzip \
    wget \
    # Add CJK fonts
    google-noto-cjk-fonts \
  && yum clean all

# Enable Python 2.7.18
ENV PATH /opt/rh/python27/root/usr/bin${PATH:+:${PATH}}
ENV LD_LIBRARY_PATH /opt/rh/python27/root/usr/lib64${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}
ENV MANPATH /opt/rh/python27/root/usr/share/man:${MANPATH}
# For systemtap
ENV XDG_DATA_DIRS /opt/rh/python27/root/usr/share:${XDG_DATA_DIRS:-/usr/local/share:/usr/share}
# For pkg-config
ENV PKG_CONFIG_PATH /opt/rh/python27/root/usr/lib64/pkgconfig${PKG_CONFIG_PATH:+:${PKG_CONFIG_PATH}}

# Remove setuid/setgid binaries from images for security
RUN find / -ignore_readdir_race -perm 6000 -type f -exec chmod a-s {} \; || true

# Set permissions on writeable directories to support arbitrary user IDs for OpenShift.
# These directories must be owned by the root group and be readable/writable by that group.
# See https://docs.openshift.com/container-platform/3.5/creating_images/guidelines.html#use-uid
RUN chmod g=u /etc/passwd

# Set an UTF-8 LANG
ENV LANG en_US.utf8

# Add a nuxeo user with a fixed UID
# We chose an arbitrary UID that doesn't conflict with possibly existing users
ENV NUXEO_USER nuxeo
RUN useradd -m -d /home/$NUXEO_USER -u 900 -s /bin/bash $NUXEO_USER

ENV NUXEO_HOME /opt/nuxeo/server
ENV NUXEO_CONF /etc/nuxeo/nuxeo.conf
ENV PATH $NUXEO_HOME/bin:$PATH

# Copy scripts and configuration files and create needed folders
# COPY --chmod only supports octal notation, using 775 that is equivalent to rwxrwxr-x
COPY --chown=900:0 --chmod=775 rootfs /

# Copy Nuxeo distribution
COPY --chown=900:0 --from=builder /distrib $NUXEO_HOME

VOLUME /var/lib/nuxeo
VOLUME /var/log/nuxeo
VOLUME /tmp

EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nuxeoctl", "console"]

# Run as a non root user with a fixed UID
USER 900

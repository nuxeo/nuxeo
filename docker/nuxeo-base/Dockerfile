# Nuxeo base image
#
# It includes some basic Open Source converters.

FROM centos:7

ARG BUILD_TAG
ARG SCM_REF
ARG VERSION

ARG LIBREOFFICE_VERSION=7.4.2

LABEL org.nuxeo.base.build-tag=$BUILD_TAG
LABEL org.nuxeo.base.scm-ref=$SCM_REF
LABEL org.nuxeo.base.version=$VERSION
# Override parent ones
LABEL org.label-schema.build-date=""
LABEL org.label-schema.license="Apache 2.0"
LABEL org.label-schema.name="Nuxeo Base"
LABEL org.label-schema.vendor="Nuxeo"
LABEL org.opencontainers.image.created=""
LABEL org.opencontainers.image.licenses="Apache 2.0"
LABEL org.opencontainers.image.title="Nuxeo Base"
LABEL org.opencontainers.image.vendor="Nuxeo"

# Configure Zulu Repository
RUN rpm --import http://repos.azulsystems.com/RPM-GPG-KEY-azulsystems \
  && rpm --install https://cdn.azul.com/zulu/bin/zulu-repo-1.0.0-1.noarch.rpm

RUN yum -y update \
  && yum -y --setopt=skip_missing_names_on_install=False install \
    # required for python27-python
    centos-release-scl-rh \
    epel-release \
    # install java first to provide it for depend packages (such as libreoffice)
    zulu11-jdk \
  # install libreoffice
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

# Set an UTF-8 LANG
ENV LANG en_US.utf8

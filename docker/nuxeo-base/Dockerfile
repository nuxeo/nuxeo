# Nuxeo base image
#
# It includes some basic Open Source converters.
#
# It uses a multi-stage build to download RPMs from our YUM registy.

# ------------------------------------------------------------------------
# RPM download stage
FROM centos:7 as rpm-download

ARG EXIFTOOL_VERSION=12.42

# Add Nexus Private Yum Release Repository
COPY target/nuxeo-private.repo /etc/yum.repos.d/

RUN yum install --downloadonly --downloaddir=/rpms \
  perl-Image-ExifTool-$EXIFTOOL_VERSION

# ------------------------------------------------------------------------
# Target stage
FROM centos:7

ARG BUILD_TAG
ARG SCM_REF
ARG VERSION

ARG LIBREOFFICE_VERSION=7.1.5

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

# Copy downloaded rpms
COPY --from=rpm-download /rpms /tmp/rpms

RUN yum -y update \
  && yum -y --setopt=skip_missing_names_on_install=False install \
    epel-release \
    # install java first to provide it for depend packages (such as libreoffice)
    zulu17-jdk \
  # install downloaded rpms
  && yum -y localinstall /tmp/rpms/* \
  && rm -rf /tmp/rpms \
  # install libreoffice
  && curl -f -L https://packages.nuxeo.com/repository/document-foundation-raw/LibreOffice_${LIBREOFFICE_VERSION}_Linux_x86-64_rpm.tar.gz | tar -C /tmp -xzv \
  && yum -y localinstall /tmp/LibreOffice_${LIBREOFFICE_VERSION}*/RPMS/*.rpm \
  && ln -s /opt/libreoffice$(echo $LIBREOFFICE_VERSION | cut -f 1,2 -d ".")/program/soffice /usr/bin/soffice \
  && rm -rf /tmp/LibreOffice_${LIBREOFFICE_VERSION}* \
  && yum -y --setopt=skip_missing_names_on_install=False install \
    ghostscript \
    ImageMagick-6.9.10.68-3.el7 \
    less \
    libwpd-tools \
    # required by exiftool to extract binary metadata from open office document
    perl-Archive-Zip \
    poppler-utils \
    tar \
    ufraw \
    unzip \
    wget \
    # Add CJK fonts
    google-noto-cjk-fonts \
  && yum clean all

# Remove setuid/setgid binaries from images for security
RUN find / -perm 6000 -type f -exec chmod a-s {} \; || true

# Set an UTF-8 LANG
ENV LANG en_US.utf8

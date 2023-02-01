# Nuxeo base image
#
# It includes some basic Open Source converters.
#
# It uses a multi-stage build to download RPMs from our YUM registy.

ARG ROCKY_LINUX_VERSION=9.1
# ------------------------------------------------------------------------
# RPM download stage
FROM rockylinux:$ROCKY_LINUX_VERSION as rpm-download

ARG EXIFTOOL_VERSION=12.42-1.el9

# Add Nexus Private Yum Release Repository
COPY target/nuxeo-private.repo /etc/yum.repos.d/

RUN dnf -y install --downloadonly --downloaddir=/rpms \
  perl-Image-ExifTool-$EXIFTOOL_VERSION

# ------------------------------------------------------------------------
# Target stage
FROM rockylinux:$ROCKY_LINUX_VERSION

ARG BUILD_TAG
ARG SCM_REF
ARG VERSION

ARG ROCKY_LINUX_VERSION=9.1
ARG LIBREOFFICE_VERSION=7.4.2
ARG GHOSTSCRIPT_MAJOR_VERSION=9
ARG GHOSTSCRIPT_MINOR_VERSION=25
ARG GHOSTSCRIPT_VERSION=$GHOSTSCRIPT_MAJOR_VERSION.$GHOSTSCRIPT_MINOR_VERSION

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

RUN dnf -y update \
  && dnf -y --setopt=skip_missing_names_on_install=False install \
    epel-release \
    # install java first to provide it for depend packages (such as libreoffice)
    zulu17-jdk \
    # find is not included in the base rockylinux image
    findutils \
    # install gzip first to allow using tar
    gzip \
  # install downloaded rpms
  && dnf -y localinstall /tmp/rpms/* \
  && rm -rf /tmp/rpms \
  # install libreoffice
  && curl -f -L https://packages.nuxeo.com/repository/document-foundation-raw/LibreOffice_${LIBREOFFICE_VERSION}_Linux_x86-64_rpm.tar.gz | tar -C /tmp -xzv \
  && dnf -y localinstall /tmp/LibreOffice_${LIBREOFFICE_VERSION}*/RPMS/*.rpm \
  && ln -s /opt/libreoffice$(echo $LIBREOFFICE_VERSION | cut -f 1,2 -d ".")/program/soffice /usr/bin/soffice \
  && rm -rf /tmp/LibreOffice_${LIBREOFFICE_VERSION}* \
  # install older version of ghostscript to avoid breaking changes on EPS to PDF conversion
  && curl -f -L https://github.com/ArtifexSoftware/ghostpdl-downloads/releases/download/gs$GHOSTSCRIPT_MAJOR_VERSION$GHOSTSCRIPT_MINOR_VERSION/ghostscript-$GHOSTSCRIPT_VERSION-linux-x86_64.tgz | tar -xzv \
  && mv ghostscript-$GHOSTSCRIPT_VERSION-linux-x86_64/gs-$GHOSTSCRIPT_MAJOR_VERSION$GHOSTSCRIPT_MINOR_VERSION-linux-x86_64 /usr/bin/gs \
  && rm -rf ghostscript-$GHOSTSCRIPT_VERSION-linux-x86_64 \
  # ImageMagick 7 for Rocky Linux 9 is only provided by a third party repo
  && dnf -y install https://rpms.remirepo.net/enterprise/remi-release-$ROCKY_LINUX_VERSION.rpm \
  && dnf -y --enablerepo=remi install ImageMagick7 \
  # devel repository required for some packages such as libwpd-tools
  && dnf -y --setopt=skip_missing_names_on_install=False --enablerepo=devel install \
    libwpd-tools \
    # required by exiftool to extract binary metadata from open office document
    perl-Archive-Zip \
    poppler-utils \
    # ps is not included in the base rockylinux image
    procps \
    unzip \
    wget \
    # Add CJK fonts
    google-noto-cjk-fonts-common \
  && dnf clean all

# Remove setuid/setgid binaries from images for security
RUN find / -ignore_readdir_race -perm 6000 -type f -exec chmod a-s {} \; || true

# Set an UTF-8 LANG
RUN dnf -y install glibc-langpack-en
ENV LANG en_US.utf8

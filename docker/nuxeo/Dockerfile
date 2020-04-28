# Nuxeo server image
#
# It includes a bare Nuxeo server distribution and basic Open Source converters.
#
# It extends the nuxeo/slim image that must be passed as build args.

ARG BASE_IMAGE

FROM ${BASE_IMAGE}
LABEL maintainer="Nuxeo <packagers@nuxeo.com>"

USER root
RUN yum -y update && yum -y install epel-release && yum -y --setopt=skip_missing_names_on_install=False install \
  ghostscript \
  ImageMagick-6.9.10.68-3.el7 \
  libreoffice-headless libreoffice-writer libreoffice-calc libreoffice-impress \
  libwpd-tools \
  # required by perl-Image-ExifTool to extract binary metadata from open office document
  perl-Archive-Zip \
  perl-Image-ExifTool \
  poppler-utils \
  ufraw \
  && yum clean all
USER 900

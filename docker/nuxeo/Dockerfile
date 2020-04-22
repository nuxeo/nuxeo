# Nuxeo Platform image
#
# It includes a bare Nuxeo server distribution with some Nuxeo packages installed.
# It includes basic Open Source converters.
#
# It uses a multi-stage build relying on a builder image and a base image.
# These images must be passed as build args.

ARG BUILDER_IMAGE
ARG BASE_IMAGE

# ------------------------------------------------------------------------
# Build stage
FROM ${BUILDER_IMAGE} as builder

# ------------------------------------------------------------------------
# Target stage
FROM ${BASE_IMAGE}
LABEL maintainer="Nuxeo <packagers@nuxeo.com>"

RUN yum -y update && yum -y install epel-release && yum -y --setopt=skip_missing_names_on_install=False install \
  ghostscript \
  ImageMagick-6.7.8.9-18.el7 \
  libreoffice-headless libreoffice-writer libreoffice-calc libreoffice-impress \
  libwpd-tools \
  # required by perl-Image-ExifTool to extract binary metadata from open office document
  perl-Archive-Zip \
  perl-Image-ExifTool \
  poppler-utils \
  ufraw \
  && yum clean all

# Copy Nuxeo distribution
COPY --from=builder /distrib $NUXEO_HOME

# Work around missing support for --chown flag with COPY instruction in Kaniko
# TODO NXP-28052: remove and use COPY --chown when fixed in Kaniko, or find a proper way
RUN chown -R 900:0 $NUXEO_HOME \
  && chmod -R g+rwX $NUXEO_HOME

# Run as a non root user with a fixed UID
USER 900

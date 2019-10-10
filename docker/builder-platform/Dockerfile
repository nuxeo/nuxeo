# Nuxeo platform builder image
#
# It provides:
# - A Nuxeo server distribution with appropriate permissions in the /distrib directory.
# - A set of pre-installed packages on the server distribution
# - An install-packages.sh script to install Nuxeo packages.
#
# It is based on an OpenJDK image as it requires Java for the package installation.
#
# It must be used within a multi-stage build.
# For instance, you can use the following Dockerfile sample to build an image containing a Nuxeo Platform distribution:
# - In the NUXEO_HOME directory.
# - With some Nuxeo packages installed.
# - Owned by the UID user and GID group.
#
# FROM nuxeo/builder-platform:VERSION as builder
# COPY local/packages /packages
# RUN install-packages.sh /packages
#
# FROM BASE_IMAGE
# COPY --from=builder --chown=UID:GID /distrib NUXEO_HOME

ARG BASE_IMAGE

FROM ${BASE_IMAGE}
LABEL maintainer="Nuxeo <packagers@nuxeo.com>"

COPY target/packages /packages
RUN install-packages.sh /packages

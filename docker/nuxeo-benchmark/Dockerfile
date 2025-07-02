# Nuxeo Benchmark image
#
# It includes a Nuxeo server with the needed packages for the Benchmark tests

ARG BASE_IMAGE

FROM $BASE_IMAGE

ARG BUILD_TAG
ARG SCM_REF
ARG VERSION

LABEL org.nuxeo.benchmark.build-tag=$BUILD_TAG
LABEL org.nuxeo.benchmark.scm-ref=$SCM_REF
LABEL org.nuxeo.benchmark.version=$VERSION
# Override parent ones
LABEL org.label-schema.name="Nuxeo Benchmark Server"
LABEL org.opencontainers.image.title="Nuxeo Benchmark Server"

RUN --mount=type=bind,source=./target/packages,target=/tmp/local-packages /install-packages.sh --offline /tmp/local-packages/*.zip

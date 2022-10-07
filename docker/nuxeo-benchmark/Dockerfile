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

COPY --chown=900:0 target/packages/*.zip /tmp/local-packages/

RUN /install-packages.sh --offline /tmp/local-packages/*.zip

# Kaniko doesn't properly support --chown on copy, thus we need to be root to delete what was copied
USER 0
RUN rm -rf /tmp/local-packages
USER 900

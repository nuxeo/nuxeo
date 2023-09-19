# Nuxeo Benchmark Docker Image

This image is intended to be used in our CI to run [Benchmark tests](../../ftests/nuxeo-server-gatling-tests). To pull the image, run:

```bash
docker pull <DOCKER_REGISTRY>/nuxeo/nuxeo-benchmark:<TAG>
```

## Build the Image

It requires to install:

- [Docker](https://docs.docker.com/install/) 19.03 or newer.
- [Docker Buildx](https://docs.docker.com/build/architecture/#buildx).

Note that [BuildKit](https://docs.docker.com/build/buildkit/) is the default builder for users on Docker Desktop and Docker Engine v23.0 and later.

There are two ways to build the image:

- With [Maven](#with-maven): suitable for local use.
- With [Skaffold](#with-skaffold): used by the [nuxeo](https://jenkins.platform.dev.nuxeo.com/job/nuxeo/job/lts/job/nuxeo/) CI pipeline, can also be used locally.

### With Maven

To build the `nuxeo/nuxeo-benchmark` image with Maven, just run:

```bash
mvn -nsu install
```

The base image is `docker.platform.dev.nuxeo.com/nuxeo/nuxeo:latest-lts-2023` by default.

To build the `nuxeo/nuxeo-benchmark` image from a `nuxeo/nuxeo:<TAG>` image hosted in another registry, run:

```bash
mvn -nsu -Ddocker.base.image=<DOCKER_REGISTRY>/nuxeo/nuxeo:<TAG> install
```

By default, the image is built for the host's architecture, e.g. `linux/amd64` or `linux/arm64`.

You can override the built platform, for instance to build a multi-architecture image:

```bash
mvn -nsu install -Ddocker.platforms=linux/amd64,linux/arm64
```

### With Skaffold

To build the `nuxeo/nuxeo-benchmark` image with Skaffold, you need to have:

- [Skaffold](https://skaffold.dev/docs/install/) installed, v2 is recommended, otherwise v1.39 is the minimum.
- The [docker-buildx](https://github.com/nuxeo/platform-builder-base/blob/main/_common/rootfs/usr/local/bin/docker-buildx) script present in your `PATH` environment variable.

Then, you need to tell Skaffold that you're building locally, otherwise it might try to push the image to a Docker registry such as docker.io if it detects a Kubernetes context.

```bash
skaffold config set --global local-cluster true
```

Fetch the needed Nuxeo packages with Maven and make them available for the Docker build:

```bash
mvn -nsu process-resources
```

Finally, run:

```bash
skaffold build
```

This builds the image described in the [skaffold.yaml](./skaffold.yaml) file and loads it inside your Docker daemon.

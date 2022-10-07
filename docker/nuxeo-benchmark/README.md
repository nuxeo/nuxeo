# Nuxeo Benchmark Docker Image

This image is intended to be used in our CI to run [Benchmark tests](../../ftests/nuxeo-server-gatling-tests). To pull the image, run:

```bash
docker pull <DOCKER_REGISTRY>/nuxeo/nuxeo-benchmark:<TAG>
```

## Build the Image

It requires to install [Docker](https://docs.docker.com/install/).

There are several ways to build the image, depending on the context:

- For a local build, use [Maven](#with-maven).
- For a pipeline running in Jenkins on Kubernetes, use [Skaffold](#with-skaffold).
- In any case, you can use [Docker](#with-docker).

### With Maven

To build the `nuxeo/nuxeo-benchmark` image locally, you need to have built the `nuxeo/nuxeo:latest-lts` image first, see its [README](../nuxeo/README.md), then run:

```bash
mvn -nsu install
```

To build the `nuxeo/nuxeo-benchmark` image locally by leveraging the `nuxeo/nuxeo:<TAG>` from another registry, run:

```bash
mvn -nsu -Ddocker.base.image=<DOCKER_REGISTRY>/nuxeo/nuxeo:<TAG> install
```

### With Skaffold

We use Skaffold to build the image as part of the [nuxeo](https://jenkins.platform.dev.nuxeo.com/job/nuxeo/job/lts/job/nuxeo/) pipeline in our Jenkins CI/CD platform.

This requires to:

- Install [Skaffold](https://skaffold.dev/docs/getting-started/#installing-skaffold).
- Install [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/).
- Configure `kubectl` to connect to a Kubernetes cluster.

It also requires the following environment variables:

- `DOCKER_REGISTRY`: the Docker registry to push the image to.
- `VERSION`: the image tag, for instance `latest-lts`.

To build the `nuxeo/nuxeo-benchmark` image with Skaffold, you first need to fetch the needed Nuxeo packages and make it available for the Docker build with Maven:

```bash
mvn -nsu process-resources
```

Then, from the module directory, run:

```bash
skaffold build -f skaffold.yaml
```

### With Docker

To build the `nuxeo/nuxeo-benchmark` image with Docker, you first need to fetch the the needed Nuxeo packages and make it available for the Docker build with Maven:

```bash
mvn -nsu process-resources
```

Then, run:

```bash
docker build --build-arg BASE_IMAGE=<DOCKER_REGISTRY>/nuxeo/nuxeo:<TAG> -t nuxeo/nuxeo-benchmark:latest-lts .
```

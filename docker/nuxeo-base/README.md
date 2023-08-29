# Nuxeo Base Docker Image

To pull the image, run:

```bash
docker pull <DOCKER_REGISTRY>/nuxeo/nuxeo-base:<TAG>
```

## Build the Image

It requires to install [Docker](https://docs.docker.com/install/).

There are several ways to build the image, depending on the context:

- For a local build, use [Maven](#with-maven).
- For a pipeline running in Jenkins on Kubernetes, use [Skaffold](#with-skaffold).
- In any case, you can use [Docker](#with-docker).

### With Maven

To build the `nuxeo/nuxeo-base` image locally, run:

```bash
mvn -nsu -DYUM_REPO_USERNAME=... -DYUM_REPO_PASSWORD=... install
```

### With Skaffold

We use Skaffold to build the image as part of the [nuxeo](https://jenkins.platform.dev.nuxeo.com/job/nuxeo/job/lts/job/nuxeo/) pipeline in our Jenkins CI/CD platform.

This requires to:

- Install [Skaffold](https://skaffold.dev/docs/getting-started/#installing-skaffold).
- Install [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/).
- Configure `kubectl` to connect to a Kubernetes cluster.

It also requires the following environment variables:

- `DOCKER_REGISTRY`: the Docker registry to push the image to.
- `VERSION`: the image tag, for instance `latest-lts-2023`.

To build the `nuxeo/nuxeo-base` image with Skaffold, you first need to give your credentials to packages.nuxeo.com and make it available for the Docker build with Maven:

```bash
mvn -nsu -DYUM_REPO_USERNAME=... -DYUM_REPO_PASSWORD=... process-resources
```

Then, from the module directory, run:

```bash
skaffold build -f skaffold.yaml
```

### With Docker

To build the `nuxeo/nuxeo-base` image with Docker, you first need to give your credentials to packages.nuxeo.com and make it available for the Docker build with Maven:

```bash
mvn -nsu -DYUM_REPO_USERNAME=... -DYUM_REPO_PASSWORD=... process-resources
```

Then, run:

```bash
docker build -t nuxeo/nuxeo-base:latest-lts-2023 .
```

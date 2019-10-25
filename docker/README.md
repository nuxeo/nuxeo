# Nuxeo Docker Images

Nuxeo provides a set of Docker images that can be separated into two categories:
- The [builder](#builder-images) images.
- The [final](#final-images) images.

The builder images allow to build a final Docker image using a multi-stage build.

The final images are ready to use.

These images are pushed to our [public Docker regsitry](https://packages.nuxeo.com/#browse/search/docker). To pull an image, run:

```
docker pull docker.packages.nuxeo.com/IMAGE_NAME:TAG
```

For instance, to pull the latest 11.1-SNAPSHOT version of the `nuxeo/nuxeo` image, run:

```
docker pull docker.packages.nuxeo.com/nuxeo/nuxeo:11.1-SNAPSHOT
```

## Disclaimer

These Docker images don't aim to replace the [Nuxeo Docker official image](https://hub.docker.com/_/nuxeo/). They implement a different approach, described [here](https://jira.nuxeo.com/browse/NXP-27514), to try having immutable images configured at build time instead of runtime.

## Builder Images

The versioning of these images follows the Nuxeo platform versioning. In the Dockerfile samples below, `VERSION` can be replaced for instance by `11.1-SNAPSHOT`.

### Base Builder: nuxeo/builder

It provides:
- A Nuxeo server distribution with appropriate permissions in the `/distrib` directory.
- An `install-packages.sh` script to install Nuxeo packages.

It is based on an OpenJDK image as it requires Java for the package installation.

It must be used within a multi-stage build.
For instance, you can use the following Dockerfile sample to build an image based on a `BASE_IMAGE`, containing a Nuxeo server distribution:
- In the `NUXEO_HOME` directory.
- With some Nuxeo packages installed, whose ZIP files must be located in the `local/packages` directory.
- Owned by the UID user and GID group.

```
FROM nuxeo/builder:VERSION as builder
COPY local/packages /packages
RUN install-packages.sh /packages

FROM BASE_IMAGE
COPY --from=builder --chown=UID:GID /distrib NUXEO_HOME
``` 

### Platform Builder: nuxeo/builder-platform

This image is similar to the [nuxeo/builder](#base-builder-nuxeobuilder) image, in addition to which it has a set of Nuxeo packages pre-installed in the Nuxeo server distribution. These packages are described by the Maven project's [POM](builder-platform/pom.xml).

In the same way, it must be used within a multi-stage build.
For instance, you can use the following Dockerfile sample to build an image based on a `BASE_IMAGE`, containing a Nuxeo Platform distribution:
 - In the NUXEO_HOME directory.
 - With the set of pre-installed Nuxeo packages mentioned above.
 - Owned by the UID user and GID group.

```
FROM nuxeo/builder-platform:VERSION as builder

FROM BASE_IMAGE
COPY --from=builder --chown=UID:GID /distrib NUXEO_HOME
```

### Nuxeo Base: nuxeo/base

This image can be used as the `BASE_IMAGE` in the Dockerfile samples seen above.

Based on CentOS 7, it includes:
- OpenJDK.
- A `nuxeo` user with the `900` fixed UID.
- The directories required to have the Nuxeo configuration, data and logs outside of the server directory, with appropriate permissions.
- An entrypoint script to configure the server.
- The default recommended volumes.
- The environment variables required by the server, typically `NUXEO_HOME` and `NUXEO_CONF`.
- The exposed port `8080`.

It doesn't contain the Nuxeo server distribution itself.
To build an image containing a Nuxeo server distribution with some packages installed, you must use a multi-stage build
with the [nuxeo/builder](#base-builder-nuxeobuilder) (or [nuxeo/builder-platform](#platform-builder-nuxeobuilder-platform)) image and the [nuxeo/base](#nuxeo-base-nuxeobase) image, as in the following Dockerfile sample:

```
FROM nuxeo/builder:VERSION as builder
COPY local/packages /packages
RUN install-packages.sh /packages

FROM nuxeo/base:VERSION
RUN yum -y install ...
COPY --from=builder --chown=900:0 /distrib $NUXEO_HOME
USER 900
```

## Final Images

### Slim: nuxeo/nuxeo

It includes a bare Nuxeo server distribution without any package installed.
It doesn't include any converter.

It is a typical example of an image built using multi-stage with the [nuxeo/builder](#base-builder-nuxeobuilder) and [nuxeo/base](#nuxeo-base-nuxeobase) images.
These images are passed as build args in the [Dockerfile](slim/Dockerfile).

### Content Platform

TODO: [NXP-28133](https://jira.nuxeo.com/browse/NXP-28133)

## Build the Images

It requires to install [Docker](https://docs.docker.com/install/).

There are several ways to build the images, depending on the context:
- For a local build, use [Maven](#with-maven).
- For a pipeline running in Jenkins X, use [Skaffold](#with-skaffold).
- In any case, you can use [Docker](#with-docker).

### With Maven

To build all the images locally, run:

```
mvn -nsu install
```

To build a single image, for instance the `nuxeo/builder` one, run:

```
mvn -nsu -f builder/pom.xml install
```

### With Skaffold
 
We use Skaffold to build the images as part of the [nuxeo](http://jenkins.platform.34.74.59.50.nip.io/job/nuxeo/job/nuxeo/) pipeline in our Jenkins X CI/CD platform.

This requires to:
- Install [Skaffold](https://skaffold.dev/docs/getting-started/#installing-skaffold).
- Install [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/).
- Configure `kubectl` to connect to a Kubernetes cluster.

It also requires the following environment variables:
- `DOCKER_REGISTRY`: the Docker registry to push the images to.
- `ORG`: the organization used as a namespace in the image names, in our case: `nuxeo`.
- `VERSION`: the image tag, for instance `11.1-SNAPSHOT`.

Because we are using the [Kaniko](https://github.com/GoogleContainerTools/kaniko) builder and there is no support (yet) for [ordering dependant builds](https://github.com/GoogleContainerTools/skaffold/issues/2717), we are relying on several `skaffold.yaml` files and performing a sequential build, see the `skaffoldBuildAll` method in the [Jenkinsfile](../Jenkinsfile).

First, to build the `nuxeo/builder` and `nuxeo/base` images, run:

```
skaffold build
```

To build the `nuxeo/builder-platform` image, run:

```
skaffold build -f builder-platform/skaffold.yaml
```

To build the `nuxeo/nuxeo` image, run:

```
skaffold build -f slim/skaffold.yaml
```

### With Docker

To build the `nuxeo/builder` image, you first need to fetch the Nuxeo distribution and make it available for the Docker build with Maven:

```
mvn -nsu -f builder/pom.xml process-resources
```

Then run:

```
docker build -t nuxeo/builder:11.1-SNAPSHOT -f builder/Dockerfile builder
```

To build the `nuxeo/base` image, run:

```
docker build -t nuxeo/base:11.1-SNAPSHOT -f base/Dockerfile base
```

To build the `nuxeo/builder-platform` image, you first need to fetch the Nuxeo packages to install and make them available for the Docker build with Maven:

```
mvn -nsu -f builder-platform/pom.xml process-resources
```

Then run:

```
docker build -t nuxeo/builder-platform:11.1-SNAPSHOT -f builder-platform/Dockerfile --build-arg BASE_IMAGE=nuxeo/builder:11.1-SNAPSHOT builder-platform
```

To build the `nuxeo/nuxeo` image, run:

```
docker build -t nuxeo/nuxeo:11.1-SNAPSHOT -f slim/Dockerfile --build-arg BUILDER_IMAGE=nuxeo/builder:11.1-SNAPSHOT --build-arg BASE_IMAGE=nuxeo/base:11.1-SNAPSHOT slim
```

## Run an Image

For instance, to run a container from the `nuxeo/nuxeo` image built locally, run:

```
docker run -it -p 8080:8080 nuxeo/nuxeo:11.1-SNAPSHOT
```

To pull the `nuxeo/nuxeo` image from our public Docker regsitry and run a container from it, run:

```
docker run -it -p 8080:8080 docker.packages.nuxeo.com/nuxeo/nuxeo:11.1-SNAPSHOT
```

## Inspect an Image

To inspect the different layers included in an image, you can run for instance:

```
docker history nuxeo/nuxeo:11.1-SNAPSHOT
```

The [dive](https://github.com/wagoodman/dive) tool is also very good for exploring an image, its layer contents and discovering ways to shrink the image size:

```
dive nuxeo/nuxeo:11.1-SNAPSHOT
```

## Configure an Image at Runtime

### Configuration Properties

TODO: [NXP-28145](https://jira.nuxeo.com/browse/NXP-28145)

### Environment Variables

Currently, the only environment variable that is taken into account by the `nuxeo/base` image or an image built from it is `NUXEO_CLID`. If provided when running a container, the value of `NUXEO_CLID` is copied to `/var/lib/nuxeo/data/instance.clid` at startup.

For instance, running:

```
docker run -it -p 8080:8080 -e NUXEO_CLID=<NUXEO_CLID> nuxeo/nuxeo:11.1-SNAPSHOT
```

allows to run a container with a registered Nuxeo instance.

Later on, with [NXP-28191](https://jira.nuxeo.com/browse/NXP-28191), we should be able to configure any Nuxeo property as an environment variable passed to the container.

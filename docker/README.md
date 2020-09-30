# Nuxeo Docker Image

Nuxeo provides a ready to use Docker image that is pushed to our Docker registry. To pull the image, run:

```bash
docker pull DOCKER_REGISTRY/nuxeo:TAG
```

## Disclaimer

This Docker image doesn't aim to replace the [Nuxeo Docker official image](https://hub.docker.com/_/nuxeo/). It implements a different approach to try having an immutable image configured at build time instead of runtime.

## Nuxeo Image

Based on CentOS 7, it includes:

- OpenJDK.
- A bare Nuxeo server without any package installed.
- Some basic Open Source converters, e.g.: ImageMagick, LibreOffice.
- A `nuxeo` user with the `900` fixed UID.
- The directories required to have the Nuxeo configuration, data and logs outside of the server directory, with appropriate permissions.
- An entrypoint script to configure the server.
- The default recommended volumes.
- The environment variables required by the server, typically `NUXEO_HOME` and `NUXEO_CONF`.
- The exposed port `8080`.

## Build the Image

It requires to install [Docker](https://docs.docker.com/install/).

There are several ways to build the image, depending on the context:

- For a local build, use [Maven](#with-maven).
- For a pipeline running in Jenkins on Kubernetes, use [Skaffold](#with-skaffold).
- In any case, you can use [Docker](#with-docker).

### With Maven

To build the `nuxeo/nuxeo` image locally, run:

```bash
mvn -nsu install
```

### With Skaffold

We use Skaffold to build the images as part of the [nuxeo](http://jenkins.platform.dev.nuxeo.com/job/nuxeo/job/nuxeo/) pipeline in our Jenkins CI/CD platform.

This requires to:

- Install [Skaffold](https://skaffold.dev/docs/getting-started/#installing-skaffold).
- Install [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/).
- Configure `kubectl` to connect to a Kubernetes cluster.

It also requires the following environment variables:

- `DOCKER_REGISTRY`: the Docker registry to push the images to.
- `VERSION`: the image tag, for instance `latest`.

To build the `nuxeo/nuxeo` image with Skaffold, you first need to fetch the Nuxeo server ZIP file and make it available for the Docker build with Maven:

```bash
mvn -nsu process-resources
```

Then, from the root directory, run:

```bash
skaffold build -f docker/skaffold.yaml
```

### With Docker

To build the `nuxeo/nuxeo` image with Docker, you first need to fetch the Nuxeo server ZIP file and make it available for the Docker build with Maven:

```bash
mvn -nsu process-resources
```

Then, run:

```bash
docker build -t nuxeo/nuxeo:latest .
```

## Run the Image

To run a container from the `nuxeo/nuxeo` image built locally, run:

```bash
docker run -it -p 8080:8080 nuxeo/nuxeo:latest
```

To pull the `nuxeo/nuxeo` image from our Docker regsitry and run a container from it, run:

```bash
docker run -it -p 8080:8080 DOCKER_REGISTRY/nuxeo/nuxeo:latest
```

## Inspect the Image

To inspect the different layers included in the image, you can run:

```bash
docker history nuxeo/nuxeo:latest
```

The [dive](https://github.com/wagoodman/dive) tool is also very good for exploring an image, its layer contents and discovering ways to shrink the image size:

```bash
dive nuxeo/nuxeo:latest
```

## Configure the Image at Runtime

Though we try to have immutable images configured at build time, in some cases it makes sense to configure a container at runtime. This typically applies to the address and credentials of each back-end store (database, Elasticsearch, S3, etc.) that are specific to a given deployment: development, staging, production, etc.

### Configuration Properties

To add some configuration properties when running a container from a Nuxeo image, you can mount property files as volumes into the `/etc/nuxeo/conf.d` directory of the container. Each property file will be appended to `nuxeo.conf` ordered by filename during startup.

For instance, to append the following `postgresql.conf` file to `nuxeo.conf`:

```Java Properties
nuxeo.db.name=nuxeo
nuxeo.db.user=nuxeo
nuxeo.db.password=nuxeo
nuxeo.db.host=localhost
nuxeo.db.port=5432
```

you can run:

```bash
docker run -it -p 8080:8080 -v /path/to/postgresql.conf:/etc/nuxeo/conf.d/postgresql.conf nuxeo/nuxeo:latest
```

### Environment Variables

Currently, these are the environment variables that are taken into account by a Nuxeo image:

- `JAVA_OPTS`
- `NUXEO_CLID`
- `NUXEO_CONNECT_URL`
- `NUXEO_PACKAGES`

Later on, with [NXP-28191](https://jira.nuxeo.com/browse/NXP-28191), we should be able to configure any Nuxeo property as an environment variable passed to the container.

#### JAVA_OPTS

The value of `JAVA_OPTS` is appended to the `JAVA_OPTS` property defined in `nuxeo.conf` at startup.

For instance, to make the Nuxeo Launcher display the JVM settings in the console, run:

```bash
docker run -it -p 8080:8080 -e JAVA_OPTS=-XshowSettings:vm nuxeo/nuxeo:latest
```

#### NUXEO_CLID

The value of `NUXEO_CLID` is copied to `/var/lib/nuxeo/instance.clid` at startup.

For instance, to run a container with a registered Nuxeo instance:

```bash
docker run -it -p 8080:8080 -e NUXEO_CLID=<NUXEO_CLID> nuxeo/nuxeo:latest
```

#### NUXEO_CONNECT_URL

`NUXEO_CONNECT_URL` allows to override the default Connect URL at startup.

For instance, to run a container with another Connect URL than the default one:

```bash
docker run -it -p 8080:8080 -e NUXEO_CONNECT_URL=<NUXEO_CONNECT_URL> nuxeo/nuxeo:latest
```

#### NUXEO_PACKAGES

`NUXEO_PACKAGES` allows to define a space separated list of Nuxeo packages to install at startup.

For instance, to run a container with the `nuxeo-web-ui` and `nuxeo-drive` packages installed:

```bash
docker run -it -p 8080:8080 -e NUXEO_CLID=<NUXEO_CLID> -e NUXEO_PACKAGES="nuxeo-web-ui nuxeo-drive" nuxeo/nuxeo:latest
```

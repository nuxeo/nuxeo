# Nuxeo Docker Image

Nuxeo provides a ready to use Docker image that is pushed to our Docker registry. To pull the image, run:

```bash
docker pull <DOCKER_REGISTRY>/nuxeo/nuxeo:<TAG>
```

## Image Content

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

### FFmpeg

As it contains some non-free codecs, FFmpeg isn't part of the Nuxeo image. However, you can build a custom Docker image, based on the Nuxeo one, including the `ffmpeg` package provided by [RPM Fusion](https://rpmfusion.org/), see the `Dockerfile` sample  below. The resulting `ffmpeg` binary embeds all the codecs required for Nuxeo video conversions.

```Dockerfile
FROM <DOCKER_REGISTRY>/nuxeo/nuxeo:<TAG>

# we need to be root to run yum commands
USER 0
# install RPM Fusion free repository
RUN yum -y localinstall --nogpgcheck https://mirrors.rpmfusion.org/free/el/rpmfusion-free-release-7.noarch.rpm
# install ffmpeg package
RUN yum -y install ffmpeg
# set back original user
USER 900
```

## Build the Image

It requires to install:

- [Docker](https://docs.docker.com/install/) 19.03 or newer.
- [Docker Buildx](https://docs.docker.com/build/architecture/#buildx).

Note that [BuildKit](https://docs.docker.com/build/buildkit/) is the default builder for users on Docker Desktop and Docker Engine v23.0 and later.

> INFO
You might need to run `docker logout` if you get errors such as:

```bash
[INFO] DOCKER> #2 [internal] load metadata for docker.io/azul/zulu-openjdk:17
[INFO] DOCKER> #2 ERROR: failed to do request: Head "https://registry-1.docker.io/v2/azul/zulu-openjdk/manifests/17": dial tcp: lookup registry-1.docker.io on 192.168.0.1:53: read udp 172.17.0.2:53228->192.168.0.1:53: i/o timeout
```

There are two ways to build the image:

- With [Maven](#with-maven): suitable for local use.
- With [Skaffold](#with-skaffold): used by the [nuxeo](https://jenkins.platform.dev.nuxeo.com/job/nuxeo/job/lts/job/nuxeo/) CI pipeline, can also be used locally.

In both cases, the build relies on the following Maven dependency for the Nuxeo server ZIP:

```xml
<dependency>
  <groupId>org.nuxeo.ecm.distribution</groupId>
  <artifactId>nuxeo-server-tomcat</artifactId>
  <type>zip</type>
</dependency>
```

If you want the Docker image to be built from the latest changes in the current repository, including the `server` Maven module, you need to start by building the Nuxeo sources with the `distrib` profile. At the root of the repository, run:

```bash
mvn -nsu install -Pdistrib -DskipTests
```

### With Maven

We use the [docker-maven-plugin](https://github.com/fabric8io/docker-maven-plugin).

To build the `nuxeo/nuxeo` image with Maven, just run:

```bash
mvn -nsu install
```

By default, the image is built for the host's architecture, e.g. `linux/amd64` or `linux/arm64`.

You can override the built platform, for instance to build a multi-architecture image:

```bash
mvn -nsu install -Ddocker.platforms=linux/amd64,linux/arm64
```

### With Skaffold

To build the `nuxeo/nuxeo` image with Skaffold, you need to have:

- [Skaffold](https://skaffold.dev/docs/install/) installed, v2 is recommended, otherwise v1.39 is the minimum.
- The [docker-buildx](https://github.com/nuxeo/platform-builder-base/blob/main/_common/rootfs/usr/local/bin/docker-buildx) script present in your `PATH` environment variable.

Then, you need to tell Skaffold that you're building locally, otherwise it might try to push the image to a Docker registry such as docker.io if it detects a Kubernetes context:

```bash
skaffold config set --global local-cluster true
```

Fetch the Nuxeo server ZIP file with Maven and make it available for the Docker build:

```bash
mvn -nsu process-resources
```

Finally, run:

```bash
skaffold build
```

This builds the image described in the [skaffold.yaml](./skaffold.yaml) file and loads it inside your Docker daemon.

## Run the Image

To run a container from the `nuxeo/nuxeo` image built locally, run:

```bash
docker run -it -p 8080:8080 nuxeo/nuxeo:latest-lts
```

To pull the `nuxeo/nuxeo` image from our Docker registry and run a container from it, run:

```bash
docker run -it -p 8080:8080 <DOCKER_REGISTRY>/nuxeo/nuxeo:<TAG>
```

## Inspect the Image

To inspect the different layers included in the image, you can run:

```bash
docker history nuxeo/nuxeo:latest-lts
```

The [dive](https://github.com/wagoodman/dive) tool is also very good for exploring an image, its layer contents and discovering ways to shrink the image size:

```bash
dive nuxeo/nuxeo:latest-lts
```

## Build a Custom Image From Nuxeo

We provide a utility script to install remote Nuxeo packages from [Nuxeo Connect](https://connect.nuxeo.com/) and local Nuxeo packages when building an image from the Nuxeo image:

For instance, you can use this script in the following `Dockerfile`:

```Dockerfile
FROM <DOCKER_REGISTRY>/nuxeo/nuxeo:<TAG>

ARG CLID
ARG CONNECT_URL

COPY --chown=900:0 path/to/local-package-nodeps-*.zip $NUXEO_HOME/local-packages/local-package-nodeps.zip
COPY --chown=900:0 path/to/local-package-*.zip $NUXEO_HOME/local-packages/local-package.zip

# Install a local package without its dependencies (`mp-install --nodeps`)
RUN /install-packages.sh --offline $NUXEO_HOME/local-packages/local-package-nodeps.zip
# Install remote packages and a local package with its dependencies
RUN /install-packages.sh --clid ${CLID} --connect-url ${CONNECT_URL} nuxeo-web-ui nuxeo-drive $NUXEO_HOME/local-packages/local-package.zip
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
docker run -it -p 8080:8080 -v /path/to/postgresql.conf:/etc/nuxeo/conf.d/postgresql.conf nuxeo/nuxeo:latest-lts
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
docker run -it -p 8080:8080 -e JAVA_OPTS=-XshowSettings:vm nuxeo/nuxeo:latest-lts
```

#### NUXEO_CLID

The value of `NUXEO_CLID` is copied to `/var/lib/nuxeo/instance.clid` at startup.

For instance, to run a container with a registered Nuxeo instance:

```bash
docker run -it -p 8080:8080 -e NUXEO_CLID=<NUXEO_CLID> nuxeo/nuxeo:latest-lts
```

#### NUXEO_CONNECT_URL

`NUXEO_CONNECT_URL` allows to override the default Connect URL at startup.

For instance, to run a container with another Connect URL than the default one:

```bash
docker run -it -p 8080:8080 -e NUXEO_CONNECT_URL=<NUXEO_CONNECT_URL> nuxeo/nuxeo:latest-lts
```

#### NUXEO_PACKAGES

`NUXEO_PACKAGES` allows to define a space separated list of Nuxeo packages to install at startup.

For instance, to run a container with the `nuxeo-web-ui` and `nuxeo-drive` packages installed:

```bash
docker run -it -p 8080:8080 -e NUXEO_CLID=<NUXEO_CLID> -e NUXEO_PACKAGES="nuxeo-web-ui nuxeo-drive" nuxeo/nuxeo:latest-lts
```

### Shell Scripts

To run some shell scripts when starting a container from a Nuxeo image, you can add
`*.sh` files in the `/docker-entrypoint-initnuxeo.d` directory of the image.

They will be alphabetically sorted and executed at the very end of the `ENTRYPOINT`, after handling the environment variables. Thus, if you run a container by passing the `NUXEO_CLID` environment variable, invoking `nuxeoctl` in such a shell script will work as being registered.

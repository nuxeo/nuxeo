FROM ubuntu:16.04
MAINTAINER Tiago Cardoso <tcardoso@nuxeo.com>

ENV GLTF_VERSION v1.0-draft2

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		ca-certificates \
		cmake \
		build-essential \
		gcc-4.7 \
		g++-4.7 \
		libpng12-dev \
		libxml2-dev \
		libpcre3-dev \
		git\
	&& rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Install glTF
RUN git clone --recurse-submodules https://github.com/KhronosGroup/glTF.git \
	&& cd glTF/COLLADA2GLTF \
	&& git checkout ${GLTF_VERSION} \
	&& cmake . \
	&& make install \
	&& cp ./bin/collada2gltf /usr/local/bin/ \
	&& rm -rf ./glTF/

# Create a working directory to mount.
VOLUME ["/in", "/out"]
ENTRYPOINT ["collada2gltf"]

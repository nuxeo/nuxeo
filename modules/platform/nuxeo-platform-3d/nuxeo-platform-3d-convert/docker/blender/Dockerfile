FROM ubuntu:16.04
MAINTAINER Tiago Cardoso <tcardoso@nuxeo.com>

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		curl \
		bzip2 \
		libfreetype6 \
		libgl1-mesa-dev \
		libglu1-mesa \
		libxrender1 \
		libxi6 \
		ca-certificates \
		unzip \
	&& apt-get -y autoremove \
	&& rm -rf /var/lib/apt/lists/*

ENV BLENDER_MAJOR 2.78
ENV BLENDER_VERSION 2.78
ENV BLENDER_BZ2_URL http://download.blender.org/release/Blender$BLENDER_MAJOR/blender-$BLENDER_VERSION-linux-glibc219-x86_64.tar.bz2

RUN mkdir /usr/local/blender \
	&& curl -SL "$BLENDER_BZ2_URL" -o blender.tar.bz2 \
	&& tar -jxvf blender.tar.bz2 -C /usr/local/blender --strip-components=1 \
	&& rm blender.tar.bz2

VOLUME ["/scripts", "/in", "/out"]
ENTRYPOINT ["/usr/local/blender/blender", "-b"]

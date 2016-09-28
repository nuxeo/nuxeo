FROM ubuntu:16.04
MAINTAINER Tiago Cardoso <tcardoso@nuxeo.com>

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		curl \
		bzip2 \
		libfreetype6 \
		libgl1-mesa-dev \
		libglu1-mesa \
		libxi6 \
		ca-certificates \
		unzip \
	&& apt-get -y autoremove \
	&& rm -rf /var/lib/apt/lists/*

ENV BLENDER_MAJOR 2.77
ENV BLENDER_VERSION 2.77a
ENV BLENDER_BZ2_URL http://mirror.cs.umn.edu/blender.org/release/Blender$BLENDER_MAJOR/blender-$BLENDER_VERSION-linux-glibc211-x86_64.tar.bz2
ENV MATERIALS_SPECIALS_URL https://github.com/meta-androcto/downloads/raw/master/AF_materials_specials.zip

RUN mkdir /usr/local/blender \
	&& curl -SL "$BLENDER_BZ2_URL" -o blender.tar.bz2 \
	&& tar -jxvf blender.tar.bz2 -C /usr/local/blender --strip-components=1 \
	&& rm blender.tar.bz2

RUN mkdir /usr/local/blender/$BLENDER_MAJOR/scripts/addons/AF_materials_specials \
	&& curl -SL "$MATERIALS_SPECIALS_URL" -o AF_materials_specials.zip \
	&& unzip AF_materials_specials.zip -d /usr/local/blender/$BLENDER_MAJOR/scripts/addons \
	&& rm AF_materials_specials.zip

VOLUME ["/scripts", "/in", "/out"]
ENTRYPOINT ["/usr/local/blender/blender", "-b"]

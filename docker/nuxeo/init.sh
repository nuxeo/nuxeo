#!/bin/bash
/docker-entrypoint.sh

nuxeoctl mp-install --offline --relax=true --accept=true /home/nuxeo/nuxeo-web-ui-3.1.15.zip /home/nuxeo/nuxeo-mysql-package-2023.25.10.zip /home/nuxeo/amazon-s3-online-storage-2023.25.10.zip

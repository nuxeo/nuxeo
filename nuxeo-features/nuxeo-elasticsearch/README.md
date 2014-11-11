nuxeo-elasticsearch
===================

## About

This is a backport of
[nuxeo-elasticsearch](https://github.com/nuxeo/nuxeo-features/tree/master/nuxeo-elasticsearch)
package available in 5.9 to 5.8

This file will only explain the difference with the master branch.

Please refer to the [master branch](https://github.com/nuxeo/nuxeo-features/tree/master/nuxeo-elasticsearch)
 for more information.

## Limitations of the 5.8 backport

- You need to install the [marketplace package](https://github.com/nuxeo/marketplace-elasticsearch/tree/5.8) 1.2.x

- This module does not support the default H2 database, you need to use a different backend

- There is no aggregates support

- Same limitations as the master impl

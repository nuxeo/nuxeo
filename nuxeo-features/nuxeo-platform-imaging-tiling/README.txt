==========================
Nuxeo Features Tiling
==========================

Features
--------

Nuxeo Features Tiling contains:
- a service that offers an API to extract pictures tiles from a blob image.
  see README.txt inside the nuxeo-platform-imaging-tiling project.
- a client module built with GWT to show a tiled image in the preview tab.

Install
-------

- You don't need to install GWT, all dependencies needed to compile
  the module will be managed by Maven.
- Copy build.properties.sample to build.properties to setup custom
  configuration (jboss path for instance)
- Run ant deploy, it'll deploy jars to nuxeo.ear/plugins

Tests
-----

- To run the tests, ImageMagick 6.3.7 or later is needed

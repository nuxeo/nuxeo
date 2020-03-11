## About nuxeo-apidoc-server

This modules provides an API to browse the Nuxeo distribution tree :

    - BundleGroup (maven group or artificial grouping)
      - Bundle
        - Component
          - Service
          - Extension Points
          - Contributions

The Nuxeo Distribution can be :

 - live : in memory (meaning runtime introspection)
 - persisted : saved in Nuxeo Repository as a tree of Documents

In addition a DocumentationService is provided to be able to :

 - extract Documentation that is built-in Nuxeo Runtime descriptors
 - extract Documentation that may be embedded inside the jar
 - add some use documentation (How to, Code samples ...)

## What it can be used for

 - browse you distribution and write some documentation
 - check that a given contribution is deployed
 - play with Nuxeo Runtime

## Parameters
 - `org.nuxeo.apidoc.site.mode`: Enable the site mode with a more user friendly design
 - `org.nuxeo.apidoc.hide.current.distribution`: Hide current distribution from the distribution listing
 - `org.nuxeo.apidoc.hide.seam.components`: Hide Seam components introspection

## Modules

This plugin is composed of 2 bundles :

 - nuxeo-apidoc-core : for the low level API
 - nuxeo-apidoc-webengine : for JAX-RS API and Webview

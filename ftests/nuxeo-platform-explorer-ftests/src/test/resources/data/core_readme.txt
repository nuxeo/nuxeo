## About nuxeo-apidoc-core

This bundle provides an API to browse the Nuxeo distribution tree :

    - BundleGroup (maven group or artificial grouping)
      - Bundle
        - Component
          - Service
          - Extension Points
          - Contributions

This API has 2 implementation :

 - org.nuxeo.apidoc.introspection : Nuxeo Runtime in memory introspection
 - org.nuxeo.apidoc.adapters : DocumentModel adapters implementing the same API

In addition a DocumentationService is provided to be able to :

 - extract Documentation that is built-in Nuxeo Runtime descriptors
 - extract Documentation that may be embedded inside the jar
 - add some use documentation (How to, Code samples ...)

#Nuxeo Binary Metadata

## General information and motivation

The **Nuxeo** addon _binary-metadata_ gives the ability to extract and rewrite binaries metadata through Nuxeo platform.
- Use by default [Exif Tool|http://www.sno.phy.queensu.ca/~phil/exiftool/]
- Let contributing other metadata processors

### Getting Started

- [Download a Nuxeo server](http://www.nuxeo.com/en/downloads) (the zip version)

- Unzip it

- Install nuxeo-binary-metadata plugin from command line
  - Linux/Mac:
    - `NUXEO_HOME/bin/nuxeoctl mp-init`
    - `NUXEO_HOME/bin/nuxeoctl mp-install nuxeo-binary-metadata`
    - `NUXEO_HOME/bin/nuxeoctl start`
  - Windows:
    - `NUXEO_HOME\bin\nuxeoctl.bat mp-init`
    - `NUXEO_HOME\bin\nuxeoctl.bat mp-install nuxeo-binary-metadata`
    - `NUXEO_HOME\bin\nuxeoctl.bat start`

- From your browser, go to `http://localhost:8080/nuxeo`

- Follow Nuxeo Wizard by clicking 'Next' buttons, re-start once completed

- Check Nuxeo correctly re-started `http://localhost:8080/nuxeo`
  - username: Administrator
  - password: Administrator

- You can now use the Binary Metadata addon with this running Nuxeo server.

###API Usage Examples:

######Services:

_work-in-progress_

######Operations:

_work-in-progress_

###Binary Metadata examples:

_work-in-progress_

###Report & Contribute

We are glad to welcome new developers on this initiative, and even simple usage feedback is great.
- Ask your questions on [Nuxeo Answers](http://answers.nuxeo.com)
- Report issues on this github repository (see [issues link](http://github.com/nuxeo/nuxeo-binary-metadata/issues) on the right)
- Contribute: Send pull requests!

##About
###Nuxeo

[Nuxeo](http://www.nuxeo.com) provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com/en/products/content-management-platform), and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management), [digital asset management](http://www.nuxeo.com/en/products/digital-asset-management), [social collaboration](http://www.nuxeo.com/en/products/social-collaboration) and [case management](http://www.nuxeo.com/en/products/case-management).

Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>

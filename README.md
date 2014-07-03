    Work in progress.........
#Nuxeo Duo Two Factors Authentication
---
## General information and motivation

The **Nuxeo** addon _nuxeo-duo-factor_ is an integration of [Duo](http://www.duosecurity.com) access in Nuxeo login plugin and provides two factors authentication through the Nuxeo login page.


### Getting Started

- [Download a Nuxeo server](http://www.nuxeo.com/en/downloads) (the zip version)

- Unzip it

- Install nuxeo-duo-factor plugin from command line
  - Linux/Mac:
    - `NUXEO_HOME/bin/nuxeoctl mp-init`
    - `NUXEO_HOME/bin/nuxeoctl mp-install nuxeo-duo-factor`
    - `NUXEO_HOME/bin/nuxeoctl start`
  - Windows:
    - `NUXEO_HOME\bin\nuxeoctl.bat mp-init`
    - `NUXEO_HOME\bin\nuxeoctl.bat mp-install nuxeo-duo-factor`
    - `NUXEO_HOME\bin\nuxeoctl.bat start`

- From your browser, go to `http://localhost:8080/nuxeo`

- Follow Nuxeo Wizard by clicking 'Next' buttons, re-start once completed

- Check Nuxeo correctly re-started `http://localhost:8080/nuxeo`
  - username: Administrator
  - password: Administrator

- You will be able to enroll at Duo and control login access through [applications or by sms/call](https://duosecurity.com/product#effective).


Note: Your machine needs internet access. If you have a proxy setting, skip the mp-init and mp-install steps at first, just do nuxeoctl start and run the wizard where you will be asked your proxy settings.

#####Login Plugin Configuration:

    in progress


###Report & Contribute

We are glad to welcome new developers on this initiative, and even simple usage feedback is great.
- Ask your questions on [Nuxeo Answers](http://answers.nuxeo.com)
- Report issues on this github repository (see [issues link](http://github.com/nuxeo/nuxeo-duo-factor/issues) on the right)
- Contribute: Send pull requests!

##About
###Nuxeo

[Nuxeo](http://www.nuxeo.com) provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com/en/products/content-management-platform), and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management), [digital asset management](http://www.nuxeo.com/en/products/digital-asset-management), [social collaboration](http://www.nuxeo.com/en/products/social-collaboration) and [case management](http://www.nuxeo.com/en/products/case-management).

Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>

###Duo
Duo signup:

- <https://signup.duosecurity.com/> -> Duo Signup

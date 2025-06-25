# Nuxeo DuoWeb Two Factors Authentication

## General information and motivation

The **Nuxeo** addon _nuxeo-duoweb-authentication_ is an integration of [Duo](https://duo.com/) access in Nuxeo login plugin and provides two factors authentication through the Nuxeo login page.

### Getting Started

- Install _nuxeo-duoweb-authentication_ Marketplace Package from command line
  - Linux/Mac:
    - `$NUXEO_HOME/bin/nuxeoctl mp-install nuxeo-duoweb-authentication`
    - `$NUXEO_HOME/bin/nuxeoctl start`
  - Windows:
    - `NUXEO_HOME\bin\nuxeoctl.bat mp-install nuxeo-duoweb-authentication`
    - `NUXEO_HOME\bin\nuxeoctl.bat start`

- Follow Login Plugin Configuration part before starting Nuxeo.

- Check Nuxeo correctly re-started `http://localhost:8080/nuxeo`
  - username: Administrator
  - password: Administrator

- You will be able to enroll at Duo and control login access through [Duo Universal Prompt](https://guide.duo.com/universal-prompt).

Note: Your machine needs internet access. If you have a proxy setting, configure it before starting Nuxeo, see the `nuxeo.http.proxy.*` [configuration parameters](https://doc.nuxeo.com/n/DyM).

### Login Plugin Configuration

You must [subscribe](https://duo.com/docs/getting-started) to Duo services and follow [Duo Web SDK documentation](https://duo.com/docs/duoweb) to create all Duo Keys.

After installing the plugin, make sure before starting to include your Duo Keys (provided by Duo) in the `nuxeo.conf` file:

```
nuxeo.duoweb.clientId=
nuxeo.duoweb.clientSecret=
nuxeo.duoweb.host=
```

`nuxeo.duoweb.clientId` and `nuxeo.duoweb.clientSecret` were named `ikey` and `skey` in previous Duo Web SDK versions.

A health check of the Duo service id done at server startup. If the Duo service is not reachable, the server won't start. This check is skipped if `nuxeo.duoweb.skipHealthCheck` is set to `true`. 

## About

### Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.

# Nuxeo DuoWeb Two Factors Authentication

## General information and motivation

The **Nuxeo** addon _nuxeo-duoweb-authentication_ is an integration of [Duo](http://www.duosecurity.com) access in Nuxeo login plugin and provides two factors authentication through the Nuxeo login page.

This plugin is available for Nuxeo Platform 5.8 and above.

### Getting Started

- [Download a Nuxeo server](http://www.nuxeo.com/en/downloads) (the zip version)

- Unzip it

- Install _nuxeo-duoweb-authentication_ Marketplace Package from command line
  - Linux/Mac:
    - `$NUXEO_HOME/bin/nuxeoctl mp-init`
    - `$NUXEO_HOME/bin/nuxeoctl mp-install nuxeo-duoweb-authentication`
    - `$NUXEO_HOME/bin/nuxeoctl start`
  - Windows:
    - `NUXEO_HOME\bin\nuxeoctl.bat mp-init`
    - `NUXEO_HOME\bin\nuxeoctl.bat mp-install nuxeo-duoweb-authentication`
    - `NUXEO_HOME\bin\nuxeoctl.bat start`

- From your browser, go to `http://localhost:8080/nuxeo`

- Follow Nuxeo Wizard by clicking 'Next' buttons, re-start once completed

- Follow Login Plugin Configuration part before restarting Nuxeo.

- Check Nuxeo correctly re-started `http://localhost:8080/nuxeo`
  - username: Administrator
  - password: Administrator

- You will be able to enroll at Duo and control login access through [applications or by sms/call](https://duosecurity.com/product#effective).

Note: Your machine needs internet access. If you have a proxy setting, skip the mp-init and mp-install steps at first, just do nuxeoctl start and run the wizard where you will be asked your proxy settings.

### Login Plugin Configuration:

You must [subscribe](https://signup.duosecurity.com/) to Duo services and follow [Duo documentation](https://www.duosecurity.com/docs/duoweb) to create all Duo Keys.

After installing the plugin, make sure before starting to include your Duo Keys (provided by Duo) in the following configuration file `NUXEO_HOME/templates/duoweb-authentication/config/duo-authentication-config.xml`:

  <?xml version="1.0"?>
  <component name="org.nuxeo.duo.factors.login.contrib">

    <require>org.nuxeo.ecm.platform.ui.web.auth.WebEngineConfig</require>

    <documentation>
      This authentication plugin processes Duo Two Factors Authentication
    </documentation>

    <extension
            target="org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService"
            point="authenticators">
      <authenticationPlugin name="DUO_TWO_FACTORS_AUTH"
                            enabled="true"
                            class="org.nuxeo.duoweb.factors.DuoFactorsAuthenticator">
        <loginModulePlugin>Trusting_LM</loginModulePlugin>
        <parameters>
          <parameter name="IKEY">YOUR_INTEGRATION_KEY</parameter>
          <parameter name="SKEY">YOUR_SECRET_KEY</parameter>
          <parameter name="AKEY">YOUR_APPLICATION_KEY</parameter>
          <parameter name="HOST">YOUR_API_HOSTNAME</parameter>
        </parameters>
      </authenticationPlugin>
    </extension>

    <extension
            target="org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService"
            point="chain">
      <authenticationChain>
        <plugins>
          <plugin>DUO_TWO_FACTORS_AUTH</plugin>
        </plugins>
      </authenticationChain>
    </extension>

    <extension point="openUrl" target="org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService">
      <openUrl name="duoFactorsPattern">
        <grantPattern>/nuxeo/duofactors.jsp</grantPattern>
      </openUrl>
    </extension>

  </component>

The `YOUR_APPLICATION_KEY` can be generated as followed in [Duo documentation](https://www.duosecurity.com/docs/duoweb#1.-generate-an-akey)

## QA results

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_nuxeo-duoweb-authentication-master)](https://qa.nuxeo.org/jenkins/job/addons_nuxeo-duoweb-authentication-master/)

### Report & Contribute

We are glad to welcome new developers on this initiative, and even simple usage feedback is great.
- Ask your questions on [Nuxeo Answers](http://answers.nuxeo.com)
- Report issues on this github repository (see [issues link](http://github.com/nuxeo/nuxeo-duoweb-authentication/issues) on the right)
- Contribute: Send pull requests!

## About
### Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.

### Duo
Duo signup:

- <https://signup.duosecurity.com/> -> DuoWeb Signup

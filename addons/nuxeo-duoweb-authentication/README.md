#Nuxeo DuoWeb Two Factors Authentication

## General information and motivation

The **Nuxeo** addon _nuxeo-duoweb-authentication_ is an integration of [DuoWeb](http://www.duosecurity.com) access in Nuxeo login plugin and provides two factors authentication through the Nuxeo login page.

This plugin is available for Nuxeo Platform 5.8 and above.

### Getting Started

- [Download a Nuxeo server](http://www.nuxeo.com/en/downloads) (the zip version)

- Unzip it

- Install _nuxeo-duoweb-authentication_ Marketplace Package from command line
  - Linux/Mac:
    - `NUXEO_HOME/bin/nuxeoctl mp-init`
    - `NUXEO_HOME/bin/nuxeoctl mp-install nuxeo-duoweb-authentication`
    - `NUXEO_HOME/bin/nuxeoctl start`
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

- You will be able to enroll at DuoWeb and control login access through [applications or by sms/call](https://duosecurity.com/product#effective).


Note: Your machine needs internet access. If you have a proxy setting, skip the mp-init and mp-install steps at first, just do nuxeoctl start and run the wizard where you will be asked your proxy settings.

###Login Plugin Configuration:

You must [subscribe](https://signup.duosecurity.com/) to DuoWeb services and follow [DuoWeb documentation](https://www.duosecurity.com/docs/duoweb) to create all DuoWeb Keys.

After installing the plugin, make sure before starting to include your DuoWeb Keys (provided by DuoWeb) in the following configuration file `NUXEO_HOME/templates/duoweb-authentication/config/duo-authentication-config.xml`:

    <?xml version="1.0"?>
  <component name="org.nuxeo.duo.factors.login.contrib">

    <require>org.nuxeo.ecm.platform.ui.web.auth.WebEngineConfig</require>

    <documentation>
      This authentication plugin processes DuoWeb Two Factors Authentication
    </documentation>

    <extension
            target="org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService"
            point="authenticators">
      <authenticationPlugin name="DUO_TWO_FACTORS_AUTH"
                            enabled="true"
                            class="org.nuxeo.duo.factors.DuoFactorsAuthenticator">
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

The `YOUR_APPLICATION_KEY` can be generated as followed in [DuoWeb documentation](https://www.duosecurity.com/docs/duoweb#1.-generate-an-akey)

###Report & Contribute

We are glad to welcome new developers on this initiative, and even simple usage feedback is great.
- Ask your questions on [Nuxeo Answers](http://answers.nuxeo.com)
- Report issues on this github repository (see [issues link](http://github.com/nuxeo/nuxeo-duoweb-authentication/issues) on the right)
- Contribute: Send pull requests!

##About
###Nuxeo

[Nuxeo](http://www.nuxeo.com) provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com/en/products/content-management-platform), and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management), [digital asset management](http://www.nuxeo.com/en/products/digital-asset-management), [social collaboration](http://www.nuxeo.com/en/products/social-collaboration) and [case management](http://www.nuxeo.com/en/products/case-management).

Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>

###DuoWeb
DuoWeb signup:

- <https://signup.duosecurity.com/> -> DuoWeb Signup

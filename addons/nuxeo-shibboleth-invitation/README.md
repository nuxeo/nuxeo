
[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=master/addons_FT_nuxeo-shibboleth-invitation-master)](https://qa.nuxeo.org/jenkins/job/master/job/addons_FT_nuxeo-shibboleth-invitation-master/)

## About Nuxeo Shibboleth Invitation

The **Nuxeo** addon _nuxeo-shibboleth-invitation_ provides the ability to invite external user to access Nuxeo Platform through basic or Shibboleth authentication.


## Building

    mvn clean install

## Getting Started

1. [Download a Nuxeo server](http://www.nuxeo.com/en/downloads) (the zip version).

2. Unzip it.

3. See Configuring Part.

4. Install nuxeo-shibboleth-invitation plugin from command line:
  
   Linux/Mac:
    - `NUXEO_HOME/bin/nuxeoctl mp-init`
    - `NUXEO_HOME/bin/nuxeoctl mp-install nuxeo-shibboleth-invitation`
    - `NUXEO_HOME/bin/nuxeoctl start`
  
   Windows:
    - `NUXEO_HOME\bin\nuxeoctl.bat mp-init`
    - `NUXEO_HOME\bin\nuxeoctl.bat mp-install nuxeo-shibboleth-invitation`
    - `NUXEO_HOME\bin\nuxeoctl.bat start`

5. From your browser, go to `http://localhost:8080/nuxeo`.

6. Follow Nuxeo Wizard by clicking **Next** buttons, re-start once completed.

7. Check Nuxeo correctly re-started `http://localhost:8080/nuxeo`.
   - Username: Administrator
   - Password: Administrator

You can now use the addon.


**Note**: Your machine needs internet access. If you have a proxy setting, skip the `mp-init` and `mp-install` steps at first. Just do `nuxeoctl start` and run the wizzard where you will be asked your proxy settings.

### Deploying

Install [the Nuxeo Shibboleth Invitation Nuxeo Package](https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-shibboleth-invitation).
Or manually copy the built artifacts into `$NUXEO_HOME/templates/custom/bundles/` and activate the "custom" template.

### Configuring

Create the following shibboleth-config.xml file in `$NUXEO_HOME/nxserver/config`:

````
<component name="sample.shibboleth.config">

  <require>org.nuxeo.ecm.platform.usermanager.UserManagerImpl</require>

  <require>org.nuxeo.ecm.platform.ui.web.auth.WebEngineConfig</require>

  <extension target="org.nuxeo.ecm.platform.shibboleth.service.ShibbolethAuthenticationService"
    point="config">
    <config>
      <uidHeaders>
        <default>uid</default>
      </uidHeaders>

      <loginURL>http://host/Shibboleth.sso/WAYF</loginURL>
      <logoutURL>http://host/Shibboleth.sso/logout</logoutURL>

      <!-- Add others fieldMappings if needed -->
      <fieldMapping header="uid">username</fieldMapping>
      <fieldMapping header="mail">email</fieldMapping>
    </config>
  </extension>
</component>
````

## Report & Contribute

We are glad to welcome new developers on this initiative, and even simple usage feedback is great.

- Ask your questions on [Nuxeo Answers](http://answers.nuxeo.com).
- Report issues on the GitHub repository (see [issues link](http://github.com/nuxeo/nuxeo-shibboleth-invitation/issues)).
- Contribute: Send pull requests!

## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.

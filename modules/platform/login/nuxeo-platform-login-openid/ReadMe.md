
## About nuxeo-platform-login-openid

This module contribute a new Login Plugin that can use OpenId to authenticate the user.

OpenId providers links will added to the login screen.

## Sample configuration for Google OpenID

You must first declare your Nuxeo Web Application to Google so that you can get the clientId and ClientSecret.

For that, go to https://code.google.com/apis/console > API Access > Create > Web Application

Once you have the clientId/clientSecret, and the accepted redirect url (like http://demo.nuxeo.com/nuxeo/nxstartup.faces?provider=GoogleOpenIDConnect&forceAnonymousLogin=true) create `nxserver/config/openid-config.xml`

    <?xml version="1.0"?>
    <component name="org.nuxeo.ecm.platform.oauth2.openid.google.testing" version="1.0">
      <require>org.nuxeo.ecm.platform.oauth2.openid.google</require>
      <extension target="org.nuxeo.ecm.platform.oauth2.openid.OpenIDConnectProviderRegistry" point="providers">
       <provider>
        <name>GoogleOpenIDConnect</name>
        <clientId><!--enter your clientId here --></clientId>
        <clientSecret><!--enter your clientSecret key here --></clientSecret>
       </provider>
      </extension>
    </component>

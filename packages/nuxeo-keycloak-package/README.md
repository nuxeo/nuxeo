# Nuxeo Platform Login Keycloak Package

This repo hosts the Keycloak plugin marketplace package for Nuxeo Platform.

## Configuration

Define in your nuxeo.conf file the following property to set up the keycloak adapter:
```
# Required
nuxeo.keycloak.realm=
nuxeo.keycloak.authServerUrl=
nuxeo.keycloak.resource=

# Optional
nuxeo.keycloak.realmPublicKey=
nuxeo.keycloak.sslRequired=
nuxeo.keycloak.confidentialPort=
nuxeo.keycloak.useResourceRoleMappings=
nuxeo.keycloak.publicClient=
nuxeo.keycloak.enableCors=
nuxeo.keycloak.corsMaxAge=
nuxeo.keycloak.corsAllowedMethods=
nuxeo.keycloak.corsAllowedHeaders=
nuxeo.keycloak.corsExposedHeaders=
nuxeo.keycloak.bearerOnly=
nuxeo.keycloak.autodetectBearerOnly=
nuxeo.keycloak.enableBasicAuth=
nuxeo.keycloak.exposeToken=
nuxeo.keycloak.credentials.secret=
nuxeo.keycloak.credentials.jwt.clientKeystoreFile=
nuxeo.keycloak.credentials.jwt.clientKeystoreType=
nuxeo.keycloak.credentials.jwt.clientKeystorePassword=
nuxeo.keycloak.credentials.jwt.clientKeyPassword=
nuxeo.keycloak.credentials.jwt.clientKeyAlias=
nuxeo.keycloak.credentials.jwt.tokenExpiration=
nuxeo.keycloak.connectionPoolSize=
nuxeo.keycloak.socketTimeoutMillis=
nuxeo.keycloak.connectionTimeoutMillis=
nuxeo.keycloak.connectionTtlMillis=
nuxeo.keycloak.disableTrustManager=
nuxeo.keycloak.allowAnyHostname=
nuxeo.keycloak.proxyUrl=
nuxeo.keycloak.truststore=
nuxeo.keycloak.truststorePassword=
nuxeo.keycloak.clientKeystore=
nuxeo.keycloak.clientKeystorePassword=
nuxeo.keycloak.clientKeyPassword=
nuxeo.keycloak.alwaysRefreshToken=
nuxeo.keycloak.registerNodeAtStartup=
nuxeo.keycloak.registerNodePeriod=
nuxeo.keycloak.tokenStore=
nuxeo.keycloak.tokenCookiePath=
nuxeo.keycloak.principalAttribute=
nuxeo.keycloak.turnOffChangeSessionIdOnLogin=
nuxeo.keycloak.tokenMinimumTimeToLive=
nuxeo.keycloak.minTimeBetweenJwksRequests=
nuxeo.keycloak.publicKeyCacheTtl=
nuxeo.keycloak.ignoreOauthQueryParameter=
nuxeo.keycloak.verifyTokenAudience=
```
See https://www.keycloak.org/docs/latest/securing_apps/#_java_adapter_config for more information.

### Example

```
nuxeo.keycloak.realm=nuxeo
nuxeo.keycloak.authServerUrl=http://localhost:8085/
nuxeo.keycloak.resource=mynuxeo
nuxeo.keycloak.publicClient=true
nuxeo.keycloak.sslRequired=external
```

## About Nuxeo

The [Nuxeo Platform](http://www.nuxeo.com/products/content-management-platform/) is an open source customizable and extensible content management platform for building business applications. It provides the foundation for developing [document management](http://www.nuxeo.com/solutions/document-management/), [digital asset management](http://www.nuxeo.com/solutions/digital-asset-management/), [case management application](http://www.nuxeo.com/solutions/case-management/) and [knowledge management](http://www.nuxeo.com/solutions/advanced-knowledge-base/). You can easily add features using ready-to-use addons or by extending the platform using its extension point system.

The Nuxeo Platform is developed and supported by Nuxeo, with contributions from the community.

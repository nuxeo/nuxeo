The tests are covering

* SSO portal authentication  (SSOPortal) : java application trusted by the nuxeo server for authenticating users.
* CAS portal authentication (CasPortal) : web application (http://localhost:9090/home) that authentify and propagate identies using CAS.

For running the tests you should setup and run a nuxeo server as follow

* get a fresh nuxeo tomcat runtime from the build system
* add the cas and portal-sso plugins in nxserver plugins folder
* add the login configuration in nxserver config (src/test/resources/SSO-config.xml)
* deploy the 3.4.4 version of CAS webapp and re-configure the spring services for accepting un-secure connection :
* reconfigure CAS for accepting un-secure connection :
   in WEB-INF/springConfiguration/ticketGrantingTicketCookieGenerator.xml remove the secure property in cookie generator
   in WEB-INF/deployerConfigContext.xml add set the property requireService to false for the bean HttpBasedServiceCredentialsAuthenticationHandler

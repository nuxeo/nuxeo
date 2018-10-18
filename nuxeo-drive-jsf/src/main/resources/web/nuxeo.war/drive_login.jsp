<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>
<%@ page import="java.security.Principal"%>
<%@ page import="org.nuxeo.ecm.core.api.NuxeoPrincipal"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.AuthenticationPluginDescriptor"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.token.TokenAuthenticator"%>
<%@ page import="org.apache.commons.httpclient.HttpStatus"%>
<%@ page import="org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService"%>
<%
Principal principal = request.getUserPrincipal();
if (principal == null) {
    response.sendError(HttpStatus.SC_UNAUTHORIZED);
    return;
}

// Don't provide token for anonymous user unless 'allowAnonymous' parameter is explicitly set to true in
// the authentication plugin configuration
if (principal instanceof NuxeoPrincipal && ((NuxeoPrincipal) principal).isAnonymous()) {
    PluggableAuthenticationService authenticationService = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
            PluggableAuthenticationService.NAME);
    AuthenticationPluginDescriptor tokenAuthPluginDesc = authenticationService.getDescriptor("TOKEN_AUTH");
    if (tokenAuthPluginDesc == null
            || !(Boolean.valueOf(tokenAuthPluginDesc.getParameters().get(TokenAuthenticator.ALLOW_ANONYMOUS_KEY)))) {
        response.sendError(HttpStatus.SC_UNAUTHORIZED);
        return;
    }
}

String userName = principal.getName();

String applicationName = request.getParameter("applicationName");
String deviceId = request.getParameter("deviceId");
String deviceDescription = request.getParameter("deviceDescription");
String permission = request.getParameter("permission");
String updateToken = request.getParameter("updateToken");

TokenAuthenticationService tokenAuthService = Framework.getLocalService(TokenAuthenticationService.class);
String token = tokenAuthService.acquireToken(userName, applicationName, deviceId, deviceDescription, permission);
%>
<html>
  <head>
    <title>Nuxeo Drive startup page</title>
    <script type="text/javascript">
      <% if (updateToken == null) { %>
      drive.create_account('<%= userName %>', '<%= token %>');
      <% } else { %>
      drive.update_token('<%= token %>');
      <% } %>
    </script>
  </head>
  <body>
    <!-- Current user [<%= userName %>] acquired authentication token [<%= token %>] -->
  </body>
</html>

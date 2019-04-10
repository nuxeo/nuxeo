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
response.setCharacterEncoding("UTF-8");
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
String useProtocol = request.getParameter("useProtocol");

TokenAuthenticationService tokenAuthService = Framework.getLocalService(TokenAuthenticationService.class);
String token = tokenAuthService.acquireToken(userName, applicationName, deviceId, deviceDescription, permission);

// If useProtocol is not in the parameters, we use code supporting the Drive versions pre-Qt5.
if (useProtocol == null) {
  response.setContentType("text/html"); %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
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
<% }

// New login system
// If the useProtocol parameter is true, this page is opened in the user's browser
// and the token is passed back to the application with its custom nxdrive protocol URL.
// If useProtocol is false (for development purposes),this page is opened using
// WebKit and the resulting JSON is parsed by Drive.
else if (Boolean.parseBoolean(useProtocol)) {
  response.sendRedirect("nxdrive://token/" + token + "/user/" + userName);
} else {
  response.setContentType("application/json"); %>
{
  "username": "<%= userName %>",
  "token": "<%= token %>"
}
<% } %>

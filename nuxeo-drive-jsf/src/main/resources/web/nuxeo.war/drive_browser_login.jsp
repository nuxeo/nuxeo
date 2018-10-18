<%@ page language="java"%>
<%@ page import="java.security.Principal"%>
<%@ page import="java.util.Locale"%>
<%@ page import="org.apache.commons.httpclient.HttpStatus"%>
<%@ page import="org.nuxeo.ecm.core.api.NuxeoPrincipal"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.AuthenticationPluginDescriptor"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.token.TokenAuthenticator"%>
<%@ page import="org.nuxeo.ecm.platform.web.common.locale.LocaleProvider"%>
<%@ page import="org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
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

Locale locale = request.getLocale();
String selectedLanguage = locale.getLanguage();

// New login system
// If the useProtocol parameter is true, this page is opened in the user's browser
// and the token is passed back to the application with its custom nxdrive protocol URL.
// If useProtocol is false (for development purposes), this page is opened with
// the requests module and the resulting JSON is parsed by Drive.
if (Boolean.parseBoolean(useProtocol)) {
  response.setContentType("text/html");
  String nxdriveUrl = "nxdrive://token/" + token + "/user/" + userName;
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<%
if (selectedLanguage != null) { %>
  <fmt:setLocale value="<%= selectedLanguage %>"/>
<%
}%>
  <fmt:setBundle basename="messages" var="messages"/>

  <head>
    <title>
      <fmt:message bundle="${messages}" key="login.nuxeoDrive.success.pageTitle" />
    </title>
    <script type="text/javascript">
      setTimeout(() => {
        window.location.replace("<%= nxdriveUrl %>");
      }, 2500);
    </script>
    <style type="text/css">
      body {
        font: normal 14px/18pt "Helvetica", Arial, sans-serif;
      }

      .container {
        font-weight: 1.3em;
        width: 80%;
        left: 10%;
        position: absolute;
        top: 45%
      }
    </style>
  </head>
  <body>
    <div class="container">
      <h1>
        <fmt:message bundle="${messages}" key="login.nuxeoDrive.success.message" />
      </h1>
      <h5>
        <fmt:message bundle="${messages}" key="login.nuxeoDrive.success.submessage">
          <fmt:param value="<%= nxdriveUrl %>"/>
        </fmt:message>
      </h5>
      <!-- Current user [<%= userName %>] acquired authentication token [<%= token %>] -->
    </div>
  </body>
</html>
<% } else {
  response.setContentType("application/json"); %>
{
  "username": "<%= userName %>",
  "token": "<%= token %>"
}
<% } %>

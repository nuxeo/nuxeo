<%@ page language="java"%>
<%@ page import="java.util.Locale"%>
<%@ page import="org.apache.http.HttpStatus"%>
<%@ page import="org.nuxeo.ecm.platform.web.common.locale.LocaleProvider"%>
<%@ page import="org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
response.setCharacterEncoding("UTF-8");

Locale locale = request.getLocale();
String selectedLanguage = locale.getLanguage();
selectedLanguage = Framework.getLocalService(LocaleProvider.class).getLocaleWithDefault(selectedLanguage).getLanguage();

TokenAuthenticationService tokenAuthService = Framework.getService(TokenAuthenticationService.class);
String token = tokenAuthService.acquireToken(request);
if (token == null) {
    response.sendError(HttpStatus.SC_UNAUTHORIZED);
    return;
}
String userName = request.getUserPrincipal().getName();
String updateToken = request.getParameter("updateToken");
String useProtocol = request.getParameter("useProtocol");

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

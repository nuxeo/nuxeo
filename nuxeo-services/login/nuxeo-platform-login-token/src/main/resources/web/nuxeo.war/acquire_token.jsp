<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
  <%@ page contentType="text/html; charset=UTF-8" %>
  <%@ page language="java" %>
  <%@ page import="org.nuxeo.runtime.api.Framework" %>
  <%@ page import="org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService" %>
  <%@ page import="org.apache.http.HttpStatus" %>
  <%
TokenAuthenticationService tokenAuthService = Framework.getService(TokenAuthenticationService.class);
String token = tokenAuthService.acquireToken(request);
if (token == null) {
    response.sendError(HttpStatus.SC_UNAUTHORIZED);
    return;
}
%>
<html>
<head>
  <script type="text/javascript">
    location.replace(location.href + '#token=<%= token %>');
  </script>
</head>
  <body>
  </body>
</html>

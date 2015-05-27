<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>
<%@ page import="java.security.Principal"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ page import="org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService"%>
<%
Principal principal = request.getUserPrincipal();
String userName = principal.getName();

String applicationName = request.getParameter("applicationName");
String deviceId = request.getParameter("deviceId");
String deviceDescription = request.getParameter("deviceDescription");
String permission = request.getParameter("permission");

TokenAuthenticationService tokenAuthService = Framework.getLocalService(TokenAuthenticationService.class);
String token = tokenAuthService.acquireToken(userName, applicationName, deviceId, deviceDescription, permission);
%>
<html>
  <head>
    <title>Nuxeo Drive startup page</title>
    <script type="text/javascript">
      drive.create_account('<%= userName %>', '<%= token %>');
    </script>
  </head>
  <body>
    <!-- Current user [<%= userName %>] acquired authentication token [<%= token %>] -->
  </body>
</html>

<%@ page language="java"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ page import="org.apache.http.HttpStatus"%>
<%@ page import="org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService"%>
<%@ page contentType="text/html; charset=UTF-8"%>
<%
TokenAuthenticationService tokenAuthService = Framework.getService(TokenAuthenticationService.class);
String token = tokenAuthService.acquireToken(request);
if (token == null) {
    response.sendError(HttpStatus.SC_UNAUTHORIZED);
    return;
}
String userName = request.getUserPrincipal().getName();
String updateToken = request.getParameter("updateToken");
%>
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


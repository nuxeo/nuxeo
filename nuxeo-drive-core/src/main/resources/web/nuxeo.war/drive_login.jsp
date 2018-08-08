<%@ page language="java"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ page import="org.apache.http.HttpStatus"%>
<%@ page import="org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService"%>
<%
response.setCharacterEncoding("UTF-8");
TokenAuthenticationService tokenAuthService = Framework.getService(TokenAuthenticationService.class);
String token = tokenAuthService.acquireToken(request);
if (token == null) {
    response.sendError(HttpStatus.SC_UNAUTHORIZED);
    return;
}
String userName = request.getUserPrincipal().getName();
String updateToken = request.getParameter("updateToken");
String useProtocol = request.getParameter("useProtocol");

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

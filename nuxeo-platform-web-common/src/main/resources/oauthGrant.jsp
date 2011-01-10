<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>

<%@page import="java.util.Map"%><html>

<body>
<%
Map<String,String> oauthInfo = (Map<String,String>) request.getSession().getAttribute("OAUTH-INFO");


if (oauthInfo==null) {
%>
 No OAuth info .... can not continue
<% } else { %>
<form action="oauth/authorize" method="POST">
Do you want to grant <%=oauthInfo.get("oauth_consumer_key")%> ?

<input name="oauth_token" type="text" value="<%=oauthInfo.get("oauth_token")%>"></input>
<input name="nuxeo_login" type="text" value="<%=request.getUserPrincipal().getName()%>"></input>

<input type="submit"></input>

</form>
<%}%>
</body>
</html>

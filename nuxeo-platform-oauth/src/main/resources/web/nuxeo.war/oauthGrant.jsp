<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>
<%@ page import="java.util.Locale"%>

<%@page import="java.util.Map"%>
<%@page import="org.nuxeo.ecm.platform.oauth.tokens.OAuthToken"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
String context = request.getContextPath();
%>
<html>
<fmt:setBundle basename="messages" var="messages"/>
<head>
<style type="text/css">
<!--
 body {
  font: normal 11px "Lucida Grande", sans-serif;
  color: #343434;
  }

.topBar {
  background:none #212325;
  width:100%;
  height:36px;
  border:0;
  }

.topBar img {
  margin-left:20px;
  }

table.loginForm {
  border-spacing:3px;
  padding:3px;
  }

.leftColumn {
  width:400px;
  }

H1 {
  color:#343434;
  font:bold 14px "Lucida Grande", sans-serif;
  padding:0;
  margin:2px 0 15px 0;
  border-bottom:1px dotted #8B8B8B;
  }

H2 {
  color:#999;
  font:bold 13px "Lucida Grande", sans-serif;
  padding:0;
  margin:0 0 0 0;
  }

.extensionButtons {
  padding:0;
  margin:0 0 0 0;
  }

.linkButtons {
  padding:0;
  margin:0 0 0 0;
  }

.login {
  background:#fff;
  opacity:0.8;
  filter : alpha(opacity=80);
  border: 1px solid #4E9AE1;
  padding:20px 75px 5px 70px;
  width:250px;
  }

.maintenanceModeMessage {
  color:red;
  font-size:12px;
 }

.warnMessage, .infoMessage {
  margin:0 0 10px;
}

.infoMessage {
  color:#b31500;
}

.login_label {
  font:bold 10px "Lucida Grande", sans-serif;
  text-align: right;
  color: #454545;
  margin:0 4px 0 0;
  width:70px;
  }

.login_input {
  border:1px inset #454545;
  background: white;
  padding:3px;
  color: #454545;
  margin:0 10px 5px 0px;
  font:normal 10px "Lucida Grande", sans-serif;
  }

/* this class is a repeat because defined in nxthemes-setup.xml but
nxthemes css is not used in login.jsp */
.login_button {
  cursor:pointer;
  color: #454545;
  font-size: 10px;
  background: none #e6e6e6;
  border:1px solid #BFC5CB;
  padding: 2px 5px 2px 5px;
  margin: 5px 10px 10px 0;
  }

.login_button:hover {
  border:1px solid #92999E;
  color:#000000;
  }

.formTitle {
  margin:0 0 20px 0;
  text-align:center;
  color:#4a4a4a;
  font-size:14px;
  }

.footer {
  color: #d6d6d6;
  font-size: 9px;
  }

.loginLegal {
  padding: 0;
  margin: 0 0 10px 0;
  }

.version {
  padding-right:50px;
  }


.labelCorp {
  margin:0;
  width:400px;
  padding-top:0px;
  }

.labelCorp ul{
  margin:0;
  padding:0 42px 0 0;
  }

.labelCorp li {
  margin:0;
  padding:0px 8px;
  list-style:none;
  float:right;
  }


.labelCorp a {
  text-decoration:none;
  color:#d7d7d7;
  font:normal 11px "Lucida Grande", sans-serif;
  padding-top:0px;
  }

.labelCorp a:hover {
  text-decoration:underline;
  }

.news_container {
  text-align:left;
}

.block_container {
  border:none;
  height:500px;
  width:365px;
  overflow:auto;
  background-color:#ffffff;
  opacity:0.8;
  filter : alpha(opacity=80);
  }

.errorMessage {
  color:#000;
  font:bold 10px "Lucida Grande", sans-serif;
  border:1px solid #666;
  background: url(<%=context%>/icons/warning.gif) 2px 3px no-repeat #FFCC33;
  margin-bottom:12px;
  display:block;
  padding:5px 5px 5px 23px;
  text-align: center;
  }
-->

</style>
</head>
<body style="margin:0;text-align:center;">

<table cellspacing="0" cellpadding="0" border="0" width="100%" height="100%">
  <tbody>
    <tr class="topBar">
      <td>
        <img width="92" height="36" alt="Nuxeo Document Management" src="<%=context%>/img/nuxeo_logo.png"/>
      </td>
    </tr>

    <tr>
 <td align="center" >
<%
OAuthToken oauthInfo = (OAuthToken) request.getSession().getAttribute("OAUTH-INFO");

if (oauthInfo==null) {
%>
 No OAuth info .... can not continue ...

<% } else { %>

<form action="oauth/authorize" method="POST">

<h1><fmt:message bundle="${messages}" key="label.oauth.consumer.application" /> <%=oauthInfo.getConsumerKey()%> <fmt:message bundle="${messages}" key="label.oauth.consumer.accessRequest" /></h1>

<h2><fmt:message bundle="${messages}" key="label.oauth.consumer.grantQuestion" /> </h2>

<br/>
<input name="oauth_token" type="hidden" value="<%=oauthInfo.getToken()%>"></input>
<input name="nuxeo_login" type="hidden" value="<%=request.getUserPrincipal().getName()%>"></input>


<table>
<tr>
<td>
<input type="submit" value ="yes"/>
</td>
<td>
<select name="duration" id="duration">
    <option value="5" >
      <fmt:message bundle="${messages}" key="label.oauth.5m" />
    </option>
    <option value="60" >
      <fmt:message bundle="${messages}" key="label.oauth.1h" />
    </option>
    <option value="1440" >
      <fmt:message bundle="${messages}" key="label.oauth.1d" />
    </option>
    <option value="10080" selected >
      <fmt:message bundle="${messages}" key="label.oauth.1w" />
    </option>
    <option value="43200" >
      <fmt:message bundle="${messages}" key="label.oauth.1mth" />
    </option>
</select>
</td>
</tr>
<tr><td>
<input type="button" value ="no"/>
</td>
<td>&nbsp;</td>
</tr>
</table>
</form>
<%}%>
</td>
</tr>

 <tr class="footer">
      <td align="center" valign="bottom">
      <div class="loginLegal">
        <fmt:message bundle="${messages}" key="label.login.copyright" />
      </div>
      </td>
    </tr>
</tbody>
</table>
</body>
</html>

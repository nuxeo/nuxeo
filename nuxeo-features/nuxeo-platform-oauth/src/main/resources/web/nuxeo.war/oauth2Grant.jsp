<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page language="java" %>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.oauth2.NuxeoOAuth2Filter" %>

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
      background: none #212325;
      width: 100%;
      height: 36px;
      border: 0;
    }

    .topBar img {
      margin-left: 20px;
    }

    H1 {
      color: #343434;
      font: bold 14px "Lucida Grande", sans-serif;
      padding: 0;
      margin: 2px 0 15px 0;
      border-bottom: 1px dotted #8B8B8B;
    }

    H2 {
      color: #999;
      font: bold 13px "Lucida Grande", sans-serif;
      padding: 0;
      margin: 0 0 0 0;
    }

    .footer {
      color: #d6d6d6;
      font-size: 9px;
    }

    .loginLegal {
      padding: 0;
      margin: 0 0 10px 0;
    }

    .labelCorp {
      margin: 0;
      width: 400px;
      padding-top: 0px;
    }

    .labelCorp ul {
      margin: 0;
      padding: 0 42px 0 0;
    }

    .labelCorp li {
      margin: 0;
      padding: 0px 8px;
      list-style: none;
      float: right;
    }

    .labelCorp a {
      text-decoration: none;
      color: #d7d7d7;
      font: normal 11px "Lucida Grande", sans-serif;
      padding-top: 0px;
    }

    .labelCorp a:hover {
      text-decoration: underline;
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
    <td align="center">
      <%
        request.getSession().setAttribute(NuxeoOAuth2Filter.USERNAME_KEY, request.getUserPrincipal().getName());
      %>

      <form action="oauth2/authorization" method="POST">
        <h2>
          <fmt:message bundle="${messages}" key="label.oauth2.grantConfirmation">
            <fmt:param value="<%= request.getSession().getAttribute(\"client_name\") %>"/>
          </fmt:message>
        </h2>

        <br/>
        <input name="response_type" type="hidden"
            value="<%= request.getSession().getAttribute("response_type")%>"/>
        <input name="client_id" type="hidden"
          value="<%= request.getSession().getAttribute("client_id")%>"/>
        <input name="redirect_uri" type="hidden"
          value="<%= request.getSession().getAttribute("redirect_uri")%>"/>
        <input name="state" type="hidden" value="<%= request.getSession().getAttribute("state") %>"/>
        <input type="button" value="no"/>
        <input type="submit" value="yes"/>
      </form>
    </td>
  </tr>

  <tr class="footer">
    <td align="center" valign="bottom">
      <div class="loginLegal">
        <fmt:message bundle="${messages}" key="label.login.copyright"/>
      </div>
    </td>
  </tr>
  </tbody>
</table>
</body>
</html>

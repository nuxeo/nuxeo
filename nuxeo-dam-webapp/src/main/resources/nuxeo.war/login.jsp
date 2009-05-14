<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!-- Nuxeo Enterprise Platform, svn $Revision: 22925 $ -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%
    String productName = Framework.getProperty("org.nuxeo.dam.product.name");
    String productVersion = Framework.getProperty("org.nuxeo.dam.product.version");
%>
<html>

<fmt:setBundle basename="messages" var="messages" />

<head>
<title><%=productName%></title>
</head>

<body style="margin: 0; text-align: center;">

<table cellspacing="0" cellpadding="0" border="0" width="100%"
  height="100%">
  <tbody>
    <tr>
      <td align="center">
      <form method="post" action="view_documents.faces">
      <!-- To prevent caching -->
      <%
          response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
          response.setHeader("Pragma", "no-cache"); // HTTP 1.0
          response.setDateHeader("Expires", -1); // Prevents caching at the proxy server
      %>
      <div class="login">
      <table>
        <tr>
          <td class="login_label"><label for="username"> <fmt:message
            bundle="${messages}" key="label.login.username" /> </label></td>
          <td><input class="login_input" type="text"
            name="user_name" id="username" size="22"></td>
        </tr>
        <tr>
          <td class="login_label"><label for="password"> <fmt:message
            bundle="${messages}" key="label.login.password" /> </label></td>
          <td><input class="login_input" type="password"
            name="user_password" id="password" size="22"></td>
        </tr>
        <tr>
          <td></td>
          <td><input type="hidden" name="form_submitted_marker"
            id="form_submitted_marker"> <input
            class="login_button" type="submit" name="Submit"
            value="<fmt:message bundle="${messages}" key="label.login.logIn" />"></td>
        </tr>
        <tr>
          <td></td>
          <td><c:if test="${param.loginFailed}">
            <div class="errorMessage"><fmt:message
              bundle="${messages}"
              key="label.login.invalidUsernameOrPassword" /></div>
          </c:if> <c:if test="${param.loginMissing}">
            <div class="errorMessage"><fmt:message
              bundle="${messages}" key="label.login.missingUsername" /></div>
          </c:if></td>
        </tr>
      </table>
      </form>
      </td>
    </tr>
    <tr class="footer">
      <td align="center" valign="bottom">
      <div class="loginLegal"><fmt:message bundle="${messages}"
        key="label.login.copyright" /></div>
      </td>
      <td align="right" class="version" valign="bottom">
      <div class="loginLegal"><%=productName%> &nbsp; <%=productVersion%></div>
      </td>
    </tr>
  </tbody>
</table>
</body>
</html>


<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!-- Nuxeo Enterprise Platform -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ page import="org.nuxeo.ecm.platform.web.common.admin.AdminStatusHelper"%>
<%@ page import="java.util.Locale"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
String productName = Framework.getProperty("org.nuxeo.ecm.product.name");
String productVersion = Framework.getProperty("org.nuxeo.ecm.product.version");
String testerName = Framework.getProperty("org.nuxeo.ecm.tester.name");
String context = request.getContextPath();

boolean maintenanceMode = AdminStatusHelper.isInstanceInMaintenanceMode();
String maintenanceMessage = AdminStatusHelper.getMaintenanceMessage();
%>

<html>
<fmt:setBundle basename="messages" var="messages"/>

<head>
<title><%=productName%></title>
<link rel="icon" type="image/png" href="<%=context%>/icons/favicon.png" />
<style type="text/css">
<!--
body {
  font: normal 12px/18pt "Lucida Grande", Arial, sans-serif;
  background: url("<%=context%>/img/login_bg.png") repeat scroll bottom left #cadfc0;
  color: #343434;
  margin: 0;
  text-align: center
}

.leftColumn {
  width: 400px
}

/* Header */
.topBar {
  background: #000 none;
  width: 100%;
  height: 36px;
  border: 0
}

.topBar img {
  margin-left: 50px
}

.labelCorp {
  margin: 0;
  width: 400px;
  padding-top: 0px
}

.labelCorp ul {
  margin: 0;
  padding: 0 42px 0 0
}

.labelCorp li {
  margin: 0;
  padding: 0px 8px;
  list-style: none;
  float: right
}

.labelCorp a {
  text-decoration: none;
  color: #d7d7d7;
  font-size: small;
  padding-top: 0px
}

.labelCorp a:hover {
  text-decoration: underline
}

/* Login block */
.login {
  background: none repeat scroll 0 0 #fff;
  border-radius: 8px;
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.5);
  margin-left: 10em;
  filter: alpha(opacity = 80);
  opacity: 0.8;
  padding: 1.5em 0 1em;
  width: 22em
}

.login_label {
  color: #454545;
  font-size: 0.8em;
  font-weight: bold;
  padding: 0 0.5em 0 0
}

.login_input {
  border: 1px solid #aaa;
  border-radius: 2px;
  box-shadow: 1px 1px 2px #e0e0e0 inset;
  padding: .3em;
  margin: 0 0 .4em;
  width: 14.5em
}

.login_button {
  background-color: #e7e7e7;
  border: 1px solid #c9C9c9;
  border-radius: 3px;
  box-shadow: 0 10px 8px #fff inset;
  color: #000;
  cursor: pointer;
  font-size: .8em;
  font-weight: bold;
  margin: 0 .9em .9em 0;
  padding: .2em .6em;
  text-decoration: none;
  text-shadow: 1px 1px 0 #fff
}

.login_button:hover {
  border: 1px solid #92999e;
  color: #000
}

/* Messages */
.maintenanceModeMessage {
  color: red;
  font-size: 12px
}

.warnMessage,.infoMessage {
  margin: 0 0 10px
}

.infoMessage {
  color: #b31500
}

.errorMessage {
  background-color: #ffe467;
  border: 1px solid #eea800;
  border-radius: 0 10px;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.2);
  color: #000;
  font-size: 0.9em;
  padding: 0.4em 1em;
  text-align: center
}

.welcome {
  background: #fff;
  opacity: 0.8;
  filter: alpha(opacity = 80);
  width: 400px;
  padding: 20px;
  margin: 10px
}

.welcomeText {
  font: 12px "Lucida Grande", sans-serif;
  text-align: left;
  color: #454545;
  margin: 0 0 0.8em
}

/* Footer */
.footer {
  color: #d6d6d6;
  font-size: .65em
}

.loginLegal {
  padding: 0;
  margin: 0 0 10px
}

.version {
  padding-right: 50px
}

/* News Container Block */
.news_container {
  text-align: left
}

.block_container {
  border: none;
  height: 500px;
  width: 365px;
  overflow: auto;
  background-color: #fff;
  opacity: 0.8;
  filter: alpha(opacity = 80)
}
-->
</style>

</head>

<body>

<table cellspacing="0" cellpadding="0" border="0" width="100%" height="100%">
  <tbody>
    <tr class="topBar">
      <td>
        <img width="92" height="36" alt="Nuxeo" src="<%=context%>/img/nuxeo_logo.png"/>
      </td>
      <td align="right" class="leftColumn">
        <div class="labelCorp">
          <ul>
            <li>
              <a onclick="window.open(this.href); return false;"
                href="http://www.nuxeo.com/en/subscription/connect?utm_source=dm&amp;utm_medium=login-page-top&amp;utm_campaign=products">
                <fmt:message bundle="${messages}" key="label.login.getSupport" />
              </a>
            </li>
            <li>
              <a onclick="window.open(this.href); return false;"
                href="http://answers.nuxeo.com/?utm_source=dm&amp;utm_medium=login-page-top&amp;utm_campaign=products">
                <fmt:message bundle="${messages}" key="label.footer.answers" />
              </a>
            </li>
            <li>
              <a onclick="window.open(this.href); return false;"
                href="http://doc.nuxeo.com/display/MAIN/Nuxeo+Documentation+Center+Home?utm_source=dm&amp;utm_medium=login-page-top&amp;utm_campaign=products">
                <fmt:message bundle="${messages}" key="label.login.documentation" />
              </a>
            </li>
          </ul>
          <div style="clear:both;" />
        </div>
      </td>
    </tr>
    <tr>
      <td align="center">
        <%@ include file="login_welcome.jsp" %>
        <form method="post" action="nxstartup.faces">
          <!-- To prevent caching -->
          <%
              response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
              response.setHeader("Pragma", "no-cache"); // HTTP 1.0
              response.setDateHeader("Expires", -1); // Prevents caching at the proxy server
          %>
          <!-- ;jsessionid=<%=request.getSession().getId()%> -->
          <!-- ImageReady Slices (login_cutted.psd) -->

          <div class="login">
            <% if (maintenanceMode) { %>
              <div class="maintenanceModeMessage">
                <div class="warnMessage">
                  <fmt:message bundle="${messages}" key="label.maintenancemode.active" /><br/>
                  <fmt:message bundle="${messages}" key="label.maintenancemode.adminLoginOnly" />
                </div>
              <div class="infoMessage">
                <fmt:message bundle="${messages}" key="label.maintenancemode.message" /> : <br/>
                <%=maintenanceMessage%>
              </div>
              </div>
            <%} %>
            <table>
             <tr>
                <td class="login_label">
                  <label for="username">
                    <fmt:message bundle="${messages}" key="label.login.username" />
                  </label>
                </td>
                <td>
                  <input class="login_input" type="text" name="user_name" id="username">
                </td>
              </tr>
              <tr>
                <td class="login_label">
                  <label for="password">
                    <fmt:message bundle="${messages}" key="label.login.password" />
                  </label>
                </td>
                <td>
                  <input class="login_input" type="password" name="user_password" id="password">
                </td>
              </tr>
              <tr>
                <td></td>
                <td>
                  <% // label.login.logIn %>
                  <input type="hidden" name="requestedUrl"
                      id="requestedUrl" value="${param.requestedUrl}">
                  <input type="hidden" name="forceAnonymousLogin"
                      id="true">
                  <input type="hidden" name="form_submitted_marker"
                      id="form_submitted_marker">
                  <input class="login_button" type="submit" name="Submit"
                    value="<fmt:message bundle="${messages}" key="label.login.logIn" />">
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <c:if test="${param.connectionFailed}">
                    <div class="errorMessage">
                      <fmt:message bundle="${messages}" key="label.login.connectionFailed" />
                    </div>
                  </c:if>
                  <c:if test="${param.loginFailed == 'true' and param.connectionFailed != 'true'}">
                    <div class="errorMessage">
                      <fmt:message bundle="${messages}" key="label.login.invalidUsernameOrPassword" />
                    </div>
                  </c:if>
                  <c:if test="${param.loginMissing}">
                    <div class="errorMessage">
                      <fmt:message bundle="${messages}" key="label.login.missingUsername" />
                    </div>
                  </c:if>
                  <c:if test="${param.securityError}">
                    <div class="errorMessage">
                      <fmt:message bundle="${messages}" key="label.login.securityError" />
                    </div>
                  </c:if>
                </td>
              </tr>
            </table>
          </div>
        </form>
      </td>
      <td class="news_container" align="right" valign="center">
        <% if (!"Nuxeo-Selenium-Tester".equals(testerName)) { %>
          <iframe class="block_container" style="visibility:hidden"
            onload="javascript:this.style.visibility='visible';"
            src="https://www.nuxeo.com/embedded/dm-login"></iframe>
        <% } %>
      </td>
    </tr>
    <tr class="footer">
      <td align="center" valign="bottom">
      <div class="loginLegal">
        <fmt:message bundle="${messages}" key="label.login.copyright">
          <fmt:param value="2012" />
        </fmt:message>
      </div>
      </td>
      <td align="right" class="version" valign="bottom">
        <div class="loginLegal">
         <%=productName%>
         &nbsp;
         <%=productVersion%>
        </div>
      </td>
    </tr>
  </tbody>
</table>

<script type="text/javascript">
  document.getElementById('username').focus();
</script>

<!--   Current User = <%=request.getRemoteUser()%> -->

</body>
</html>


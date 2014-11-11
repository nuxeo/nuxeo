<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!-- Nuxeo Enterprise Platform -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ page import="org.nuxeo.ecm.platform.web.common.admin.AdminStatusHelper"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.LoginScreenConfig"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.LoginProviderLink"%>
<%@ page import="java.lang.Boolean"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Locale"%>
<%@ page import="org.joda.time.DateTime"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%
String productName = Framework.getProperty("org.nuxeo.ecm.product.name");
String productVersion = Framework.getProperty("org.nuxeo.ecm.product.version");
String testerName = Framework.getProperty("org.nuxeo.ecm.tester.name");
String context = request.getContextPath();

// Read Seam locale cookie
String localeCookieName = "org.jboss.seam.core.Locale";
Cookie localeCookie = null;
Cookie cookies[] = request.getCookies();
if (cookies != null) {
  for (int i = 0; i < cookies.length; i++) {
    if (localeCookieName.equals(cookies[i].getName())) {
      localeCookie = cookies[i];
      break;
    }
  }
}
String selectedLanguage = null;
if (localeCookie != null) {
    selectedLanguage = localeCookie.getValue();
}

boolean maintenanceMode = AdminStatusHelper.isInstanceInMaintenanceMode();
String maintenanceMessage = AdminStatusHelper.getMaintenanceMessage();

LoginScreenConfig screenConfig = LoginScreenHelper.getConfig();
List<LoginProviderLink> providers = screenConfig.getProviders();
boolean useExternalProviders = providers!=null && providers.size()>0;

// fetch Login Screen config and manage default
boolean showNews = screenConfig.getDisplayNews();
String iframeUrl = screenConfig.getNewsIframeUrl();

String bodyBackgroundStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getBodyBackgroundStyle(), "url('" + context + "/img/login_bg.jpg') no-repeat center center fixed #333");
String headerStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getHeaderStyle(), "");
String loginBoxBackgroundStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getLoginBoxBackgroundStyle(), "none repeat scroll 0 0 #fff");
String footerStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getFooterStyle(), "");
boolean disableBackgroundSizeCover = Boolean.TRUE.equals(screenConfig.getDisableBackgroundSizeCover());

String logoWidth = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoWidth(), "113");
String logoHeight = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoHeight(), "20");
String logoAlt = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoAlt(), "Nuxeo");
String logoUrl = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoUrl(), context + "/img/nuxeo_logo.png");
String currentYear = new DateTime().toString("Y");

%>

<html>
<%
if (selectedLanguage != null) { %>
<fmt:setLocale value="<%= selectedLanguage %>"/>
<%
}%>
<fmt:setBundle basename="messages" var="messages"/>

<head>
<title><%=productName%></title>
<link rel="icon" type="image/png" href="<%=context%>/icons/favicon.png" />
<link rel="shortcut icon" type="image/x-icon" href="<%=context%>/icons/favicon.ico" />
<script type="text/javascript" src="<%=context%>/scripts/detect_timezone.js"></script>
<script type="text/javascript" src="<%=context%>/scripts/nxtimezone.js"></script>
<script type="text/javascript">
  nxtz.resetTimeZoneCookieIfNotSet();
</script>
<style type="text/css">
<!--
html, body {
  height: 100%;
  overflow: hidden;
}

body {
  font: normal 12px/18pt "Lucida Grande", Arial, sans-serif;
  background: <%=bodyBackgroundStyle%>;
  color: #343434;
  margin: 0;
  <% if (!disableBackgroundSizeCover) { %>
  -webkit-background-size: cover;
  -moz-background-size: cover;
  -o-background-size: cover;
  background-size: cover;
  <%} %>
}

.leftColumn {
  width: 400px
}

/* Header */
.topBar {
  background: #fff none;
  box-shadow: 1px 0 4px rgba(0, 0, 0, 0.4);
  width: 100%;
  height: 40px;
  border: 0;
  <%=headerStyle%>;
}

.topBar img {
  margin-left: 50px
}

.labelCorp {
  margin: 0;
  width: 400px;
  padding-top: 0
}

.labelCorp ul {
  margin: 0;
  padding: 0 42px 0 0
}

.labelCorp li {
  margin: 0;
  padding: 0 8px;
  list-style: none;
  float: right
}

.labelCorp a {
  text-decoration: none;
  color: #213f7d;
  font-size: small;
  padding-top: 0
}

.labelCorp a:hover {
  text-decoration: underline
}

/* Login block */
.login {
  background: <%=loginBoxBackgroundStyle%>;
  box-sizing: border-box;
  -moz-box-sizing: border-box;
  border-radius: 3px;
  filter: alpha(opacity = 90);
  opacity: .9;
  padding: 1.5em 1em 1em;
  width: 300px }

.login_label {
  color: #454545;
  font-size: 12px;
  font-weight: bold;
  padding: 0 .5em 0 0
}

.login_input {
  border: 1px solid #aaa;
  border-radius: 2px;
  box-shadow: 1px 1px 2px #e0e0e0 inset;
  padding: .3em;
  margin: 0 0 .4em;
  width: 160px
}

.login_button {
  background-color: #e7e7e7;
  border: 1px solid #c9C9c9;
  border-radius: 3px;
  box-shadow: 0 10px 8px #fff inset;
  color: #000;
  cursor: pointer;
  font-size: 12px;
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

/* Other ids */
.loginOptions {
  border-top: 1px solid #ccc;
  color: #999;
  font-size: .85em;
}

.loginOptions p {
  margin: 1em .5em .7em;
  font-size: .95em;
}

.idList {
  padding: 0 1em;
}

.idItem {
  display: inline;
}

.idItem a, .idItem a:visited {
  background: url(<%=context%>/icons/default.png) no-repeat 5px center #eee;
  border: 1px solid #ddd;
  border-radius: 3px;
  color: #666;
  display: inline-block;
  font-weight: bold;
  margin: .3em 0;
  padding: .1em .2em .1em 2em;
  text-decoration: none;
}

.idItem a:hover {
  background-color: #fff;
  color: #333;
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

.feedbackMessage {
  border-bottom: 1px dotted #ccc;
  color: #a0a0a0;
  font-size: .7em;
  margin-bottom: 1em;
  padding: 0 0 .5em;
  text-align: center }

.errorMessage {
  color: #f40000 }

.welcome {
  background: none repeat scroll 0 0 #fff;
  bottom: 3%;
  margin: 10px;
  opacity: 0.8;
  padding: 20px;
  position: absolute;
  width: 60%;
}
.welcomeText {
  font: 12px "Lucida Grande", sans-serif;
  text-align: left;
  color: #454545;
  margin: 0 0 .8em
}

/* Footer */
.footer {
  color: #d6d6d6;
  font-size: .65em;
  height:35px;
  <%=footerStyle%>
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
  opacity: .8;
  filter: alpha(opacity = 80)
}
-->
</style>

</head>

<body>
<!-- Locale: <%= selectedLanguage %> -->
<table cellspacing="0" cellpadding="0" border="0" width="100%" height="100%" class="container">
  <tbody>
    <tr class="topBar">
      <td align="left">
        <img width="<%=logoWidth%>" height="<%=logoHeight%>" alt="<%=logoAlt%>" src="<%=logoUrl%>" />
      </td>
      <td align="right" class="leftColumn">
        <div class="labelCorp">
          <ul>
            <li>
              <a onclick="window.open(this.href); return false;"
                href="//www.nuxeo.com/en/subscription/connect?utm_source=dm&amp;utm_medium=login-page-top&amp;utm_campaign=products">
                <fmt:message bundle="${messages}" key="label.login.getSupport" />
              </a>
            </li>
            <li>
              <a onclick="window.open(this.href); return false;"
                href="//answers.nuxeo.com/?utm_source=dm&amp;utm_medium=login-page-top&amp;utm_campaign=products">
                <fmt:message bundle="${messages}" key="label.footer.answers" />
              </a>
            </li>
            <li>
              <a onclick="window.open(this.href); return false;"
                href="//doc.nuxeo.com/display/MAIN/Nuxeo+Documentation+Center+Home?utm_source=dm&amp;utm_medium=login-page-top&amp;utm_campaign=products">
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
               <td colspan="2">
                 <c:if test="${param.nxtimeout}">
                   <div class="feedbackMessage">
                     <fmt:message bundle="${messages}" key="label.login.timeout" />
                   </div>
                 </c:if>
                 <c:if test="${param.connectionFailed}">
                   <div class="feedbackMessage errorMessage">
                     <fmt:message bundle="${messages}" key="label.login.connectionFailed" />
                   </div>
                 </c:if>
                 <c:if test="${param.loginFailed == 'true' and param.connectionFailed != 'true'}">
                   <div class="feedbackMessage errorMessage">
                     <fmt:message bundle="${messages}" key="label.login.invalidUsernameOrPassword" />
                   </div>
                 </c:if>
                 <c:if test="${param.loginMissing}">
                   <div class="feedbackMessage errorMessage">
                     <fmt:message bundle="${messages}" key="label.login.missingUsername" />
                   </div>
                 </c:if>
                 <c:if test="${param.securityError}">
                   <div class="feedbackMessage errorMessage">
                     <fmt:message bundle="${messages}" key="label.login.securityError" />
                   </div>
                 </c:if>
               </td>
             </tr>
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
                <td align="left">
                  <% // label.login.logIn %>
                  <% if (selectedLanguage != null) { %>
                  <input type="hidden" name="language"
                      id="language" value="<%= selectedLanguage %>" />
                  <% } %>
                  <input type="hidden" name="requestedUrl"
                      id="requestedUrl" value="${fn:escapeXml(param.requestedUrl)}" />
                  <input type="hidden" name="forceAnonymousLogin"
                      id="true" />
                  <input type="hidden" name="form_submitted_marker"
                      id="form_submitted_marker" />
                  <input class="login_button" type="submit" name="Submit"
                    value="<fmt:message bundle="${messages}" key="label.login.logIn" />" />
                </td>
              </tr>
            </table>

            <% if (useExternalProviders) {%>
            <div class="loginOptions">
              <p><fmt:message bundle="${messages}" key="label.login.loginWithAnotherId" /></p>
              <div class="idList">
                <% for (LoginProviderLink provider : providers) { %>
                <div class="idItem">
                  <a href="<%= provider.getLink(request, request.getContextPath() + request.getParameter("requestedUrl")) %>"
                    style="background-image:url('<%=(context + provider.getIconPath())%>')" title="<%=provider.getDescription()%>"><%=provider.getLabel()%>
                  </a>
                </div>
                <%}%>
              </div>
            </div>
            <%}%>

          </div>
        </form>
      </td>
      <td class="news_container" align="right" valign="middle">
        <% if (showNews && !"Nuxeo-Selenium-Tester".equals(testerName)) { %>
          <iframe class="block_container" style="visibility:hidden"
            onload="javascript:this.style.visibility='visible';"
            src="<%=iframeUrl%>"></iframe>
        <% } %>
      </td>
    </tr>
    <tr class="footer">
      <td align="center" valign="bottom">
      <div class="loginLegal">
        <fmt:message bundle="${messages}" key="label.login.copyright">
          <fmt:param value="<%=currentYear %>" />
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

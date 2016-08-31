<!DOCTYPE html>
<!-- Nuxeo Enterprise Platform -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>
<%@ page import="java.util.List"%>
<%@ page import="org.joda.time.DateTime"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.LoginProviderLink"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.LoginScreenConfig"%>
<%@ page import="org.nuxeo.ecm.platform.web.common.admin.AdminStatusHelper"%>
<%@ page import="org.nuxeo.common.Environment"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.LoginVideo" %>
<%@ page import="org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%
String productName = Framework.getProperty(Environment.PRODUCT_NAME);
String productVersion = Framework.getProperty(Environment.PRODUCT_VERSION);
String testerName = Framework.getProperty("org.nuxeo.ecm.tester.name");
String itunesId = Framework.getProperty("nuxeo.mobile.application.itunesId");
String baseURL = VirtualHostHelper.getBaseURL(request);
boolean isTesting = "Nuxeo-Selenium-Tester".equals(testerName);
String context = request.getContextPath();

HttpSession httpSession = request.getSession(false);
if (httpSession!=null && httpSession.getAttribute(NXAuthConstants.USERIDENT_KEY)!=null) {
  response.sendRedirect(context + "/" + LoginScreenHelper.getStartupPagePath());
}

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

String backgroundPath = LoginScreenHelper.getValueWithDefault(screenConfig.getBackgroundImage(), context + "/img/login_bg.jpg");
String bodyBackgroundStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getBodyBackgroundStyle(), "url('" + backgroundPath + "') no-repeat center center fixed #006ead");
String loginButtonBackgroundColor = LoginScreenHelper.getValueWithDefault(screenConfig.getLoginButtonBackgroundColor(), "#ff452a");
String headerStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getHeaderStyle(), "");
String loginBoxBackgroundStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getLoginBoxBackgroundStyle(), "none repeat scroll 0 0");
String footerStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getFooterStyle(), "");
boolean disableBackgroundSizeCover = Boolean.TRUE.equals(screenConfig.getDisableBackgroundSizeCover());
String fieldAutocomplete = screenConfig.getFieldAutocomplete() ? "on" : "off";

String logoWidth = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoWidth(), "113");
String logoHeight = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoHeight(), "20");
String logoAlt = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoAlt(), "Nuxeo");
String logoUrl = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoUrl(), context + "/img/login_logo.png");
String currentYear = new DateTime().toString("Y");

boolean hasVideos = screenConfig.hasVideos();
String muted = screenConfig.getVideoMuted() ? "muted " : "";
String loop = screenConfig.getVideoLoop() ? "loop " : "";
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

<meta name="apple-itunes-app"
  content="app-id=<%=itunesId%>, app-argument=<%=baseURL%>" />
<meta name="viewport" content="width=device-width, initial-scale=1">

<style type="text/css">
<!--
* {
  box-sizing: border-box;
  -webkit-box-sizing: border-box;
}
html, body {
  height: 100%;
  overflow: hidden;
  margin: 0;
  padding: 0;
}
body {
  font: normal 14px/18pt "Lucida Grande", Arial, sans-serif;
  background: <%=bodyBackgroundStyle%>;
  color: #343434;
  <% if (!disableBackgroundSizeCover) { %>
  -webkit-background-size: cover;
  -moz-background-size: cover;
  -o-background-size: cover;
  background-size: cover;
  <%} %>
}
video {
  position: fixed;
  top: 50%;
  left: 50%;
  min-width: 100%;
  min-height: 100%;
  width: auto;
  height: auto;
  z-index: -100;
  transform: translateX(-50%) translateY(-50%);
  -moz-transform: translateX(-50%) translateY(-50%);
  -webkit-transform: translateX(-50%) translateY(-50%);
  background: url('<%=backgroundPath%>') no-repeat;
  background-size: cover;
  transition: 1s opacity;
}
.container {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: stretch;
  align-content: stretch;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  width: 100%;
  height: 100%;
}
header {
  <%=headerStyle%>;
}
footer {
  color: #d6d6d6;
  font-size: 1em;
  text-align: center;
  <%=footerStyle%>;
}
header, footer {
  padding: 1em 1.5em;
}
section {
  flex: 1 1 auto;
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: stretch;
  align-content: stretch;
  -webkit-box-flex: 1;
  display: -webkit-box;
  -webkit-box-align: center;
}
.main {
  flex: 2 1 65%;
  display: flex;  
  align-items: center;
  justify-content: center;
  -webkit-box-flex: 2;
  display: -webkit-box;
  -webkit-box-pack: center;
}
.news {
  flex: 1 1 35%;
  display: flex;
  align-items: center;
  justify-content: center;
  -webkit-box-flex: 1;
  -webkit-box-pack: center;
  min-width: 400px;
}
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
  margin: 0 0 .8em;
}
form {
  display: flex;
  flex-direction: column;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  width: 18em;
  margin: 0;
  padding: 1em;
  background: <%=loginBoxBackgroundStyle%>;
}
form > * {
  flex: 1 1 auto;
  -webkit-box-flex: 1;
  width: 100%;
}
.login_input {
  border: 0;
  border-radius: 2px;
  box-shadow: 1px 1px 2px #e0e0e0 inset, 0 1px 2px rgba(0,0,0,0.2);
  padding: .7em;
  margin: 0 0 .4em;
  font-size:115%;
}
.login_button {
  border: 0;
  background-color: <%= loginButtonBackgroundColor %>;
  color: white;
  cursor: pointer;
  font-size: 115%;
  font-weight: normal;
  padding: 0.7em 1.2em;
  text-decoration: none;
  /* Remove iOS corners and glare on inputs */
  -webkit-appearance: none;
  -webkit-border-radius: 0;
}
.login_button:hover {
  box-shadow: 0 -4px 0 rgba(0, 0, 0, 0.3) inset;
}
/* Other ids */
.loginOptions {
  border-top: 1px solid #ccc;
  color: #d6d6d6;
  font-size: .85em;
}
.loginOptions p {
  margin: 1em .5em .7em;
  font-size: .95em;
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
.maintenanceModeMessage {
  color: #f40000;
  font-size: 12px;
}
.warnMessage,.infoMessage {
  margin: 0 0 10px;
}
.infoMessage {
  color: #b31500;
}
.feedbackMessage {
  background-color: rgba(255,255,255,0.8);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  border-radius: 2px;
  color: #00adff;
  font-size: 95%;
  margin-bottom: .4em;
  padding: .5em;
  text-align: center;
}
.errorMessage {
  color: #f40000;
}
.news-container {
  border: none;
  height: 500px;
  width: 365px;
  overflow: auto;
  background-color: rgba(255,255,255,0.7);
  opacity: .8;
  filter: alpha(opacity = 80);
}

/* Mobile devices */
@media all and (max-device-width: 800px) {
  body {
    height: auto;
    background-image: none;
    background-position: center center;
  }
  header {
    text-align: center;
  }
  footer, video, .news, welcome {
    display: none;
  }
}
-->
</style>

</head>

<body>
<% if (hasVideos && !isTesting) { %>
<video autoplay <%= muted + loop %> preload="auto" poster="<%=backgroundPath%>" id="bgvid">
  <% for (LoginVideo video : screenConfig.getVideos()) { %>
  <source src="<%= video.getSrc() %>" type="<%= video.getType() %>">
  <% } %>
</video>
<% } %>
<!-- Locale: <%= selectedLanguage %> -->
<div class="container">
  <header>
    <img width="<%=logoWidth%>" height="<%=logoHeight%>" alt="<%=logoAlt%>" src="<%=logoUrl%>" />
  </header>
  <section>
    <div class="main">
      <%@ include file="login_welcome.jsp" %>
      <form method="post" action="startup" autocomplete="<%= fieldAutocomplete %>">
        <!-- To prevent caching -->
        <%
          response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
          response.setHeader("Pragma", "no-cache"); // HTTP 1.0
          response.setDateHeader("Expires", -1); // Prevents caching at the proxy server
        %>
        <!-- ;jsessionid=<%=request.getSession().getId()%> -->
        <% if (maintenanceMode) { %>
          <div class="maintenanceModeMessage">
            <div class="warnMessage">
              <fmt:message bundle="${messages}" key="label.maintenancemode.active" /><br/>
              <fmt:message bundle="${messages}" key="label.maintenancemode.adminLoginOnly" />
            </div>
            <div class="infoMessage">
              <fmt:message bundle="${messages}" key="label.maintenancemode.message" /> : <br/>
            </div>
          </div>
        <%} %>
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
        <input class="login_input" type="text" name="user_name" id="username"
          placeholder="<fmt:message bundle="${messages}" key="label.login.username" />"/>
        <input class="login_input" type="password" name="user_password" id="password"
          placeholder="<fmt:message bundle="${messages}" key="label.login.password" />">
        <% if (selectedLanguage != null) { %>
        <input type="hidden" name="language" id="language" value="<%= selectedLanguage %>" />
        <% } %>
        <input type="hidden" name="requestedUrl" id="requestedUrl" value="${fn:escapeXml(param.requestedUrl)}" />
        <input type="hidden" name="forceAnonymousLogin" id="true" />
        <input type="hidden" name="form_submitted_marker" id="form_submitted_marker" />
        <input class="login_button" type="submit" name="Submit"
          value="<fmt:message bundle="${messages}" key="label.login.logIn" />" />
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
      </form>
    </div>
  	<div class="news">
    <% if (showNews && !isTesting) { %>
      <iframe id="news" class="news-container" style="visibility:hidden"
        onload="javascript:this.style.visibility='visible';"
        data-src="<%=iframeUrl%>"></iframe>
    <% } %>
    </div>
  </section>
  <footer>
    <fmt:message bundle="${messages}" key="label.login.copyright">
      <fmt:param value="<%=currentYear %>" />
    </fmt:message>
    <%=productName%>
    &nbsp;
    <%=productVersion%>
  </footer>
</div>

<script type="text/javascript">
  document.getElementById('username').focus();
  // Don't load iframe on mobile devices
  if (window.matchMedia("(min-device-width: 800px)").matches) {
  	newsIframe = document.getElementById('news');
  	newsIframe.src = newsIframe.getAttribute('data-src');
  }
</script>

<!--   Current User = <%=request.getRemoteUser()%> -->

</body>
</html>

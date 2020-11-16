<!DOCTYPE html>
<!-- Nuxeo Enterprise Platform -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Locale"%>
<%@ page import="org.apache.commons.lang3.StringUtils"%>
<%@ page import="org.apache.commons.lang3.StringEscapeUtils"%>
<%@ page import="org.joda.time.DateTime"%>
<%@ page import="org.nuxeo.ecm.core.api.repository.RepositoryManager"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper"%>
<%@ page import="org.nuxeo.ecm.platform.web.common.MobileBannerHelper"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.LoginProviderLink"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.LoginScreenConfig"%>
<%@ page import="org.nuxeo.ecm.platform.web.common.admin.AdminStatusHelper"%>
<%@ page import="org.nuxeo.common.Environment"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.LoginVideo" %>
<%@ page import="org.nuxeo.ecm.platform.web.common.locale.LocaleProvider"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%
String productName = Framework.getProperty(Environment.PRODUCT_NAME);
String productVersion = Framework.getProperty(Environment.PRODUCT_VERSION);
String testerName = Framework.getProperty("org.nuxeo.ecm.tester.name");
boolean isTesting = "Nuxeo-Selenium-Tester".equals(testerName);
String context = request.getContextPath();

HttpSession httpSession = request.getSession(false);
if (httpSession!=null && httpSession.getAttribute(NXAuthConstants.USERIDENT_KEY)!=null) {
  response.sendRedirect(context + "/" + LoginScreenHelper.getStartupPagePath());
}

Locale locale = request.getLocale();
String selectedLanguage = locale.getLanguage();
selectedLanguage = Framework.getLocalService(LocaleProvider.class).getLocaleWithDefault(selectedLanguage).getLanguage();

boolean maintenanceMode = AdminStatusHelper.isInstanceInMaintenanceMode();
String maintenanceMessage = AdminStatusHelper.getMaintenanceMessage();

LoginScreenConfig screenConfig = LoginScreenHelper.getConfig();
List<LoginProviderLink> providers = screenConfig.getProviders();
boolean useExternalProviders = providers!=null && providers.size()>0;

// fetch Login Screen config and manage default
boolean showNews = screenConfig.getDisplayNews();
String iframeUrl = screenConfig.getNewsIframeUrl();

String backgroundPath = LoginScreenHelper.getValueWithDefault(screenConfig.getBackgroundImage(), context + "/img/login-bg.svg");
String bodyBackgroundStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getBodyBackgroundStyle(), "url('" + backgroundPath + "') center no-repeat #f2f2f2;");
String loginButtonBackgroundColor = LoginScreenHelper.getValueWithDefault(screenConfig.getLoginButtonBackgroundColor(), "#0066ff");
String loginBoxBackgroundStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getLoginBoxBackgroundStyle(), "#fff");
String footerStyle = LoginScreenHelper.getValueWithDefault(screenConfig.getFooterStyle(), "");
boolean disableBackgroundSizeCover = Boolean.TRUE.equals(screenConfig.getDisableBackgroundSizeCover());
String fieldAutocomplete = screenConfig.getFieldAutocomplete() ? "on" : "off";

String logoWidth = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoWidth(), "116");
String logoHeight = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoHeight(), "22");
String logoAlt = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoAlt(), "Nuxeo");
String logoUrl = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoUrl(), context + "/img/login_logo.png");
String currentYear = new DateTime().toString("Y");

boolean hasVideos = screenConfig.hasVideos();
String muted = screenConfig.getVideoMuted() ? "muted " : "";
String loop = screenConfig.getVideoLoop() ? "loop " : "";

boolean displayMobileBanner = !"false".equals(request.getParameter("displayMobileBanner"));
%>

<html class="no-js">
<%
if (selectedLanguage != null) { %>
<fmt:setLocale value="<%= selectedLanguage %>"/>
<%
}%>
<fmt:setBundle basename="messages" var="messages"/>

<head>
<meta charset="utf-8">
<title><%=productName%></title>
<link rel="icon" type="image/png" href="<%=context%>/icons/favicon.png" />
<link rel="shortcut icon" type="image/x-icon" href="<%=context%>/icons/favicon.ico" />
<script type="text/javascript" src="<%=context%>/scripts/detect_timezone.js"></script>
<script type="text/javascript" src="<%=context%>/scripts/nxtimezone.js"></script>
<% if (displayMobileBanner) { %>
<script type="text/javascript" src="<%=context%>/scripts/mobile-banner.js"></script>
<% } %>
<script type="text/javascript">
  nxtz.resetTimeZoneCookieIfNotSet();
</script>

<meta name="viewport" content="width=device-width, initial-scale=1">

  <style type="text/css">
  @font-face {
    font-display: block;
    font-family: "Inter";
    font-style: normal;
    font-weight: 400;
    src: url("https://rsms.me/inter/font-files/Inter-Regular.woff2?v=3.15") format("woff2"), url("https://rsms.me/inter/font-files/Inter-Regular.woff?v=3.15") format("woff");
  }
  @font-face {
    font-display: block;
    font-family: "Inter";
    font-style: italic;
    font-weight: 400;
    src: url("https://rsms.me/inter/font-files/Inter-Italic.woff2?v=3.15") format("woff2"), url("https://rsms.me/inter/font-files/Inter-Italic.woff?v=3.15") format("woff");
  }
  @font-face {
    font-display: swap;
    font-family: "Inter";
    font-style: normal;
    font-weight: 600;
    src: url("https://rsms.me/inter/font-files/Inter-SemiBold.woff2?v=3.15") format("woff2"), url("https://rsms.me/inter/font-files/Inter-SemiBold.woff?v=3.15") format("woff");
  }
  @font-face {
    font-display: swap;
    font-family: "Inter";
    font-style: italic;
    font-weight: 600;
    src: url("https://rsms.me/inter/font-files/Inter-SemiBoldItalic.woff2?v=3.15") format("woff2"), url("https://rsms.me/inter/font-files/Inter-SemiBoldItalic.woff?v=3.15") format("woff");
  }
  /* Variable font. */
  @font-face {
    font-display: swap;
    font-family: "Inter var";
    font-named-instance: "Regular";
    font-style: normal;
    font-weight: 100 900;
    src: url("https://rsms.me/inter/font-files/Inter-roman.var.woff2?v=3.15") format("woff2");
  }
  @font-face {
    font-display: swap;
    font-family: "Inter var";
    font-named-instance: "Italic";
    font-style: italic;
    font-weight: 100 900;
    src: url("https://rsms.me/inter/font-files/Inter-italic.var.woff2?v=3.15") format("woff2");
  }

  * {
    box-sizing: border-box;
  }

  html,
  form * {
    font: normal 16px/1.5 "Inter", sans-serif;
  }

  @supports (font-variation-settings: normal) {
    html,
  form * {
      font-family: "Inter var", sans-serif;
    }
  }
  html,
  body {
    height: 100%;
    margin: 0;
    padding: 0;
  }

  body {
    background: <%=bodyBackgroundStyle%>;
    color: #000;
    <% if (!disableBackgroundSizeCover) { %>
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
    background: url("<%=backgroundPath%>") center no-repeat;
    background-size: cover;
    transition: 1s opacity;
  }

  .container {
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
    align-items: stretch;
    align-content: stretch;
    width: 100%;
    height: 100%;
  }

  .header {
    margin-bottom: 2.5rem;
    text-align: center;
  }

  footer {
    color: #000;
    font-size: 0.75rem;
    letter-spacing: 10%;
    line-height: 2;
    padding: 2rem;
    text-align: center;
    text-transform: uppercase;
    <%=footerStyle%>
  }

  section {
    flex: 1 1 auto;
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: stretch;
    align-content: stretch;
  }
  section > div {
    margin: 0 56px;
    max-width: 500px;
    width: 100%;
  }

  .mobile-apps {
    display: none;
  }

  .main {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .news {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .welcome {
    background: rgba(255, 255, 255, 0.8);
    bottom: 4rem;
    left: 0;
    padding: 1.5rem;
    position: absolute;
    width: 60%;
  }

  .welcomeText {
    color: #000;
    font-size: 0.75rem;
    margin: 0 0 1rem;
    text-align: left;
  }

  form {
    align-items: center;
    background: <%=loginBoxBackgroundStyle%>;
    border-radius: 4px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    margin: 0;
    min-height: 350px;
    padding: 1rem;
    width: 100%;
  }

  form > * {
    max-width: 350px;
    width: 100%;
  }

  .login_input {
    background-color: rgba(229, 240, 255, 0.4);
    border: 1px solid transparent;
    padding: 0.7em;
    margin: 0 0 1rem;
  }

  .login_input:hover,
  .login_input:focus {
    border-color: #2e9cff;
    color: #000;
    outline: none;
  }

  input:-webkit-autofill {
    -webkit-text-fill-color: #000;
  }

  input:-webkit-autofill:focus {
    -webkit-text-fill-color: #000;
  }

  .login_button {
    background-color: <%= loginButtonBackgroundColor %>;
    border-radius: 4px;
    border: 0;
    color: #fff;
    cursor: pointer;
    font-weight: 600;
    padding: 0.75rem 1.25rem;
    text-decoration: none;
    transition: background-color 0.2s ease;
    /* Remove iOS corners and glare on inputs */
    -webkit-appearance: none;
  }

  .login_button:hover,
  .login_button:focus {
    background-color: rgba(0, 102, 255, 0.7);
  }

  /* Other ids */
  .loginOptions {
    border-top: 1px solid #ccc;
    color: #d6d6d6;
    font-size: 0.875rem;
  }

  .loginOptions p {
    margin: 1em 0.5em 0.7em;
    font-size: 0.75rem;
  }

  .idItem {
    display: inline;
  }

  .idItem a,
  .idItem a:visited {
    background: url(<%=context%>/icons/default.png) no-repeat 5px center #eee;
    border: 1px solid #ddd;
    border-radius: 3px;
    color: #666;
    display: inline-block;
    font-weight: 600;
    margin: 0.3em 0;
    padding: 0.1em 0.2em 0.1em 2em;
    text-decoration: none;
  }

  .idItem a:hover {
    background-color: #fff;
    color: #333;
  }

  .maintenanceModeMessage {
    color: #f40000;
    font-size: 0.75rem;
  }

  .warnMessage,
  .infoMessage {
    margin: 0 0 10px;
  }

  .infoMessage {
    color: #b31500;
  }

  .feedbackMessage {
    background-color: rgba(255, 255, 255, 0.8);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
    border-radius: 2px;
    color: #00adff;
    font-size: 0.875rem;
    margin-bottom: 0.4em;
    padding: 0.5em;
    text-align: center;
  }

  .errorMessage {
    color: #f40000;
  }

  .news-container {
    background: transparent;
    border: 0;
    border-radius: 4px;
    height: 350px;
    overflow: auto;
    width: 100%;
  }

  <% if (displayMobileBanner) { %>
  /* =Mobile Banner */
  #mobileBanner {
    /* set to block to display the mobile banner */
    display: none;
    margin-top: -1rem;
    padding: 0 0 0.6rem;
    text-align: center;
  }

  a.mobileAppLink {
    -webkit-appearance: none;
       -moz-appearance: none;
            appearance: none;
    background-color: #fff;
    border: 1px solid #000;
    border-radius: 4px;
    color: #000;
    cursor: pointer;
    display: inline-block;
    font-weight: 600;
    letter-spacing: 0.1px;
    line-height: 1.5;
    padding: 0.375rem 1.25rem;
    text-decoration: none;
    transition: background-color 0.2s ease, border-color 0.2s ease;
  }
  a.mobileAppLink:hover {
    background-color: #bcbfbf;
    border-color: #4a4a4a;
  }
  
  <% } %>

  .show-for-ios,
  .show-for-android {
    display: none;
  }

  .is-ios .show-for-ios,
  .is-android .show-for-android {
    display: inline;
  }

  /* Mobile devices */
  @media all and (max-width: 850px) {
    body {
      background: none;
      min-height: 100vh;
      overflow: auto;
    }

    .container {
      background: none;
    }

    .mobile-apps {
      align-items: center;
      display: flex;
      flex: 1 1 auto;
      justify-content: center;
    }

    .news,
    video,
   .welcome {
      display: none;
    }

    form {
      min-height: inherit;
    }
  }
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
  <section>
    <div class="main">
      <%@ include file="login_welcome.jsp" %>
      <form method="post" action="startup" autocomplete="<%= fieldAutocomplete %>">
        <div class="header">
          <img width="<%=logoWidth%>" height="<%=logoHeight%>" alt="<%=logoAlt%>" src="<%=logoUrl%>" />
        </div>
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
        <input class="login_input" type="text" name="user_name" id="username" autocomplete="off"
          placeholder="<fmt:message bundle="${messages}" key="label.login.username" />"/>
        <input class="login_input" type="password" name="user_password" id="password" autocomplete="off"
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
    <% if (showNews && !isTesting) { %>
    <div class="news">
      <iframe id="news" class="news-container" style="visibility: hidden"
        onload="javascript:this.style.visibility='visible';"
        data-src="<%=iframeUrl%>"></iframe>
    </div>
    <% } %>
  </section>
  <div class="mobile-apps">
    <div>
      <a href="https://itunes.apple.com/app/nuxeo/id1103802613" target="_blank" rel="noopener" class="show-for-ios">
        <img class="app-icons" src="https://www.nuxeo.com/assets/imgs/icons/app-store.svg" alt="Apple App Store"
          height="45" />
      </a>
      <a href="https://play.google.com/store/apps/details?id=com.nuxeomobile" target="_blank" rel="noopener" class="show-for-android">
        <img class="app-icons" src="https://www.nuxeo.com/assets/imgs/icons/google-play-badge.png" alt="Google Play"
          height="45" />
      </a>
    </div>
  </div>
  <footer>
    <fmt:message bundle="${messages}" key="label.login.copyright">
      <fmt:param value="<%=currentYear %>" />
    </fmt:message>
    <%=productName%>
    &nbsp;
    <%=productVersion%>
  </footer>
</div>
<% if (displayMobileBanner) {
    String androidApplicationURL = MobileBannerHelper.getURLForAndroidApplication(request);
    String iOSApplicationURL = MobileBannerHelper.getURLForIOSApplication(request);
    String appStoreURL = MobileBannerHelper.getAppStoreURL();
    RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
    String defaultRepositoryName = repositoryManager.getDefaultRepositoryName();

    String requestedUrlFragment = null;
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if (NXAuthConstants.START_PAGE_FRAGMENT_KEY.equals(cookie.getName())) {
                requestedUrlFragment = StringEscapeUtils.escapeEcmaScript(cookie.getValue());
            }
        }
    }
%>
<div id="mobileBanner">
  <a id="androidAppLink" class="mobileAppLink" href="<%=androidApplicationURL%>">
    <fmt:message bundle="${messages}" key="label.mobile.openInApp" />
  </a>
  <a id="iOSAppLink" class="mobileAppLink"
    data-action="<%=iOSApplicationURL%>"
    onclick="nuxeo.mobile.openIOSAppOrAppStore(this.getAttribute('data-action'), '<%=appStoreURL%>');">
    <fmt:message bundle="${messages}" key="label.mobile.openInApp" />
  </a>
</div>

<script type="text/javascript">
  // Build mobile app links if the mobile banner is displayed
  <% if (StringUtils.isNotBlank(requestedUrlFragment)) { %>

  var urlFragment = decodeURIComponent('<%=requestedUrlFragment%>');
  var docPart;
  if (urlFragment.startsWith('!/browse')) {
    // no repository name for 'browse' URL
    docPart = urlFragment.replace('!/browse/', '<%=defaultRepositoryName%>/path/');
  } else {
    // !/doc/ URL
    var parts = urlFragment.split('/');
    if (parts.length === 3) {
      // no server in URL
      docPart = "<%=defaultRepositoryName%>/id/" + parts[2];
    } else if (parts.length === 4) {
      docPart = parts[2] + "/id/" + parts[3];
    }
  }

  if (docPart) {
    var androidAppLink = document.getElementById('androidAppLink');
    var iOSAppLink = document.getElementById('iOSAppLink');
    androidAppLink.href += docPart;
    iOSAppLink.setAttribute('data-action', iOSAppLink.getAttribute('data-action') + docPart);
  }

  nuxeo.mobile.displayMobileBanner('mobileBanner', 'block', 'androidAppLink', 'iOSAppLink');
  <% } %>
</script>
<% } %>

<script type="text/javascript">
  document.getElementById('username').focus();

  <% if (showNews && !isTesting) { %>
  // Don't load iframe on mobile devices
  if (window.matchMedia("(min-device-width: 850px)").matches) {
    newsIframe = document.getElementById('news');
    if (newsIframe) {
      newsIframe.src = newsIframe.getAttribute('data-src');
    }
  }
  <% } %>

  <% if (displayMobileBanner) { %>
  if (window.matchMedia("(max-device-width: 850px)").matches) {
    // Add classes for app stores
    var htmlClasses = [];
    if (/iPhone|iPad|iPod/.test(window.navigator.userAgent)) {
      htmlClasses.push('is-ios');
    }
    if (/Android/.test(window.navigator.userAgent)) {
      htmlClasses.push('is-android');
    }
    document.documentElement.className = htmlClasses.join(' ');
  }
  <% } %>
</script>

<!--   Current User = <%=request.getRemoteUser()%> -->

</body>
</html>

<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8"%>

<%@ page import="org.nuxeo.common.Environment"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.LoginScreenConfig"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%
String context = request.getContextPath();

String productName = Framework.getProperty(Environment.PRODUCT_NAME);

LoginScreenConfig screenConfig = LoginScreenHelper.getConfig();
String loginButtonBackgroundColor = LoginScreenHelper.getValueWithDefault(screenConfig.getLoginButtonBackgroundColor(), "#0066ff");
String logoWidth = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoWidth(), "113");
String logoHeight = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoHeight(), "20");
String logoAlt = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoAlt(), "Nuxeo");
String logoUrl = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoUrl(), context + "/img/login_logo.png");

%>
<html>
<fmt:setBundle basename="messages" var="messages"/>

<head>
<title><%=productName%></title>
<link rel="icon" type="image/png" href="<%=context%>/icons/favicon.png" />
<link rel="shortcut icon" type="image/x-icon" href="<%=context%>/icons/favicon.ico" />

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
    font: normal 16px/22pt "Lucida Grande", Arial, sans-serif;
    background: #f2f2f2;
    color: #343434;
  }

  header {
    background-color: #fff;
    text-align: center;
    padding: .8em 1em .3em;
  }

  header img {
    max-height: 3em;
  }

  .container {
    display: flex;
    align-items: center;
    justify-content: center;
    height: calc(100% - 10em);
  }

  form {
    margin: 0;
    background-color: #fff;
    width: 100%;
    padding: 1.7em;
  }

  form > * {
    flex: 1 1 auto;
    -webkit-box-flex: 1;
    width: 100%;
  }

  .buttons {
    display: flex;
    justify-content: space-between;
    margin: 2em 0 0;
  }

  .button {
    flex-basis: 46%;
    border: 0;
    background-color: #f2f2f2;
    cursor: pointer;
    font-weight: normal;
    line-height: 1.5em;
    font-size: 1em;
    padding: .7em 1.2em;
    text-decoration: none;
    /* Remove iOS corners and glare on inputs */
    -webkit-appearance: none;
    -webkit-border-radius: 0;
  }

  .button:hover,
  .button:focus {
    box-shadow: 0 -5px 0 rgba(0, 0, 0, 0.3) inset;
    outline: none;
  }

  .button.primary {
    background-color: <%= loginButtonBackgroundColor %>;
    color: white;
    font-weight: bold;
  }

  button.disabled {
    cursor: not-allowed;
  }

  @media all and (min-width: 500px) {
    form {
      padding: 2.5em;
      width: 40em;
    }
  }
-->
</style>

</head>

<body>
<header>
  <img width="<%=logoWidth%>" height="<%=logoHeight%>" alt="<%=logoAlt%>" src="<%=logoUrl%>" />
</header>
<div class="container">
  <form id="oauth2Form" action="<%=context%>/oauth2/authorize_submit" method="POST">
    <!-- To prevent caching -->
    <%
      response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
      response.setHeader("Pragma", "no-cache"); // HTTP 1.0
      response.setDateHeader("Expires", -1); // Prevents caching at the proxy server
    %>

    <fmt:message bundle="${messages}" key="label.oauth2.grantConfirmation">
      <fmt:param value="${client_name}"/>
      <fmt:param value="<%=productName%>"/>
    </fmt:message>

    <input name="response_type" type="hidden" value="${fn:escapeXml(response_type)}"/>
    <input name="client_id" type="hidden" value="${fn:escapeXml(client_id)}"/>
    <% if (request.getAttribute("redirect_uri") != null) { %>
    <input name="redirect_uri" type="hidden" value="${fn:escapeXml(redirect_uri)}"/>
    <% } %>
    <% if (request.getAttribute("scope") != null) { %>
    <input name="scope" type="hidden" value="${fn:escapeXml(scope)}"/>
    <% } %>
    <% if (request.getAttribute("state") != null) { %>
    <input name="state" type="hidden" value="${fn:escapeXml(state)}"/>
    <% } %>
    <% if (request.getAttribute("code_challenge") != null && request.getAttribute("code_challenge_method") != null) { %>
    <input name="code_challenge" type="hidden" value="${fn:escapeXml(code_challenge)}"/>
    <input name="code_challenge_method" type="hidden" value="${fn:escapeXml(code_challenge_method)}"/>
    <% } %>

    <div class="buttons">
      <button class="button" name="deny_access" value="1">
        <fmt:message bundle="${messages}" key="label.oauth2.cancel" />
      </button>
      <button class="button primary" name="grant_access" value="1">
        <fmt:message bundle="${messages}" key="label.oauth2.allow" />
      </button>
    </div>
  </form>
</div>

<script type="text/javascript">
  var submitted = false;
  var oauth2Form = document.getElementById("oauth2Form");
  oauth2Form.onsubmit = function(evt) {
    if (submitted) {
      evt.preventDefault();
    } else {
      [].slice.call(oauth2Form.getElementsByTagName("button")).forEach(function(elt) {
        elt.classList.add("disabled");
      });
      submitted = true;
    }
    return true;
  };
</script>

</body>
</html>

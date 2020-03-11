<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8"%>

<%@ page import="org.nuxeo.common.Environment"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.service.LoginScreenConfig"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
String context = request.getContextPath();
String productName = Framework.getProperty(Environment.PRODUCT_NAME);
LoginScreenConfig screenConfig = LoginScreenHelper.getConfig();
String logoWidth = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoWidth(), "113");
String logoHeight = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoHeight(), "20");
String logoAlt = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoAlt(), "Nuxeo");
String logoUrl = LoginScreenHelper.getValueWithDefault(screenConfig.getLogoUrl(), context + "/img/login_logo.png");
%>
<html>
<fmt:setBundle basename="messages" var="messages"/>

<head>
<title><%=productName%> - 400</title>
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
  .error {
    margin: 0;
    background-color: #fff;
    width: 100%;
    padding: 1.7em;
  }
  .error > * {
    flex: 1 1 auto;
    -webkit-box-flex: 1;
    width: 100%;
  }
  @media all and (min-width: 500px) {
    .error {
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
    <div class="error">
      <h1>
        <fmt:message bundle="${messages}" key="label.oauth2.invalidRequest.title" />
      </h1>
      <fmt:message bundle="${messages}" key="label.oauth2.invalidRequest.message" />

      <div class="detail">
        <h4>
          <fmt:message bundle="${messages}" key="label.oauth2.invalidRequest.details.title" />
        </h4>
        <code>${error.description}</code>
      </div>
    </div>
  </div>
</body>
</html>

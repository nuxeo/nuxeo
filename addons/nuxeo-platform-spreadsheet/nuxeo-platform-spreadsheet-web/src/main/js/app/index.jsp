<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
  String selectedLanguage = "en";
  // Read Seam locale cookie
  String localeCookieName = "org.jboss.seam.core.Locale";
  Cookie localeCookie = null;
  Cookie cookies[] = request.getCookies();
  if (cookies != null) {
    for (Cookie cooky : cookies) {
      if (localeCookieName.equals(cooky.getName())) {
        localeCookie = cooky;
        break;
      }
    }
  }
  if (localeCookie != null) {
    selectedLanguage = localeCookie.getValue();
  }
%>
<html>
<fmt:setLocale value="<%= selectedLanguage %>"/>
<fmt:setBundle basename="messages" var="messages"/>
<head>
  <title>Nuxeo Spreadsheet</title>
  <!-- build:css styles/vendor.css -->
  <link rel="stylesheet" href="bower_components/jquery-ui/themes/smoothness/jquery-ui.css">
  <link rel="stylesheet" href="bower_components/jquery-ui/themes/smoothness/theme.css">
  <link rel="stylesheet" href="bower_components/handsontable/dist/jquery.handsontable.full.css">
  <link rel="stylesheet" href="bower_components/select2/select2.css">
  <!-- endbuild -->
  <!-- build:css styles/style.css -->
  <link rel="stylesheet" href="styles/styles.css">
  <!-- endbuild -->
  <!-- build:js scripts/vendor.js -->
  <script src="bower_components/jquery/dist/jquery.js"></script>
  <script src="bower_components/jquery-ui/ui/core.js"></script>
  <script src="bower_components/jquery-ui/ui/datepicker.js"></script>
  <script src="bower_components/handsontable/dist/jquery.handsontable.full.js"></script>
  <script src="bower_components/select2/select2.js"></script>
  <script src="lib/select2-editor.js"></script>
  <script src="bower_components/nuxeo/lib/jquery/nuxeo.js"></script>
  <script src="bower_components/traceur/traceur.js"></script>
  <script src="bower_components/es6-module-loader/dist/es6-module-loader.js"></script>
  <script src="bower_components/system.js/lib/extension-register.js"></script>
  <!-- endbuild -->
</head>
<body>
  <div id="header">
    <fmt:message bundle="${messages}" key="title.spreadsheet" />
  </div>

  <div id="queryArea" style="display:none">
    <input id="query" type="text" placeholder="SELECT * FROM Document" />
    <button id="execute" class="button"><fmt:message bundle="${messages}" key="command.execute" /></button>
  </div>

  <div id="grid"></div>

  <div class="buttonsGadget">
    <label><input type="checkbox" name="autosave" autocomplete="off"><fmt:message bundle="${messages}" key="label.spreadsheet.autoSave" /></label>
    <div style="display:inline-block;width:16px">
        <img id="loading" style="display:none" src="images/ajax-loader.gif">
    </div>
    <button id="save"><fmt:message bundle="${messages}" key="command.save" /></button>
    <button id="close" style="display:none"><fmt:message bundle="${messages}" key="command.close" /></button>
    <div id="console" class="console"></div>
  </div>
  <script>register(System);</script>
  <!-- build:js scripts/app.js -->
  <script src="app-build.js"></script>
  <!-- endbuild -->
  <script>
    var nuxeo = nuxeo || {};
    nuxeo.spreadsheet = {
      language: '<%= selectedLanguage %>',
      labels: {
        "saving": "<fmt:message bundle="${messages}" key="message.spreadsheet.saving" />",
        "failedSave": "<fmt:message bundle="${messages}" key="message.spreadsheet.failedSave" />",
        "upToDate": "<fmt:message bundle="${messages}" key="message.spreadsheet.upToDate" />",
        "rowsSaved": "<fmt:message bundle="${messages}" key="message.spreadsheet.rowsSaved" />",
        "autoSave": "<fmt:message bundle="${messages}" key="message.spreadsheet.autoSave" />"
      }
    };
    var contextPath = '<%= request.getContextPath() %>';
    System.import('app')
      .then(function(app) { return app.run(contextPath); })
      .then(function() { window.nuxeoSpreadsheetReady = true; })
      .catch(console.error.bind(console));
  </script>
</body>
</html>

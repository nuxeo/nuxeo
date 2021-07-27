<%@ page contentType="text/javascript; charset=UTF-8" %>
<%@ page trimDirectiveWhitespaces="true" %>
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
<fmt:setLocale value="<%= selectedLanguage %>"/>
<fmt:setBundle basename="messages" var="messages"/>
var nuxeo = nuxeo || {};
nuxeo.spreadsheet = {
  baseURL: '<%= request.getContextPath() %>',
  language: '<%= selectedLanguage %>',
  labels: {
    "saving": "<fmt:message bundle="${messages}" key="message.spreadsheet.saving" />",
    "failedSave": "<fmt:message bundle="${messages}" key="message.spreadsheet.failedSave" />",
    "upToDate": "<fmt:message bundle="${messages}" key="message.spreadsheet.upToDate" />",
    "rowsSaved": "<fmt:message bundle="${messages}" key="message.spreadsheet.rowsSaved" />",
    "autoSave": "<fmt:message bundle="${messages}" key="message.spreadsheet.autoSave" />"
  }
};

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page language="java" %>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.plugins.AnonymousAuthenticator"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<html>
<fmt:setBundle basename="messages" var="messages"/>
  <head>
  <title>Nuxeo : Acces right limited</title>

  </head>
  <body>
    <script type="text/javascript">

    var indexEndServerName = location.href.indexOf('/', 8);
    var indexEndBaseURL = location.href.indexOf('/', indexEndServerName + 2);
    var logoutURL = location.href.substring(0, indexEndBaseURL) + '/logout';

    var obj = 'window.location.replace("' + logoutURL + '");';
    setTimeout(obj,0);

    </script>
  </body>
</html>

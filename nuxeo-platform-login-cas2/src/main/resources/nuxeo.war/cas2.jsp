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

     // var obj = 'window.location.replace("http://127.0.0.1:8080/cas/login?service=http://127.0.0.1:8080/nuxeo/nxstartup.faces");'; 
     var obj = 'window.location.replace("http://127.0.0.1:8080/nuxeo/logout");';
     setTimeout(obj,1);

    </script>

      You will be redirect to the correct place 
  </body>
</html> 
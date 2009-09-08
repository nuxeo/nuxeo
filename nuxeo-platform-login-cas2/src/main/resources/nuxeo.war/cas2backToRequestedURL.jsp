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

     var cookies = document.cookie.split(';');
     var urlToReach = '/nuxeo/nxstartup.faces';
     document.write('location : ' + window.location + '<br>')
     var i;
     var cookieValue;
     var cookieName;
     for (i = 0; i < cookies.length; i++) {
       if (cookies[i] != undefined) {
         cookieName = cookies[i].split('=')[0];
         cookieValue = cookies[i].substr(cookieName.length + 1, cookies[i].length - cookieName.length);
         if (cookieName == 'cookie.name.url.to.reach.from.sso') {
        	 urlToReach = cookieValue;
         }
       }
     }

     var d = new Date();

     document.cookie = "cookie.name.url.to.reach.from.sso=removed;path=/;expires= " + d.toGMTString() + ";";

     var obj = 'window.location.replace("' + urlToReach + '");';
     setTimeout(obj,1);

    </script>

      Redirection to URL requested 
  </body>
</html> 
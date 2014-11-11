<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page language="java" %>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.plugins.AnonymousAuthenticator"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
String context = request.getContextPath();
%>
<html>
<fmt:setBundle basename="messages" var="messages"/>
<head>
  <title>Nuxeo Error Page</title>
  <style type="text/css">
<!--
body {
  background: url(<%=context%>/img/fond_error.gif ) 0 0 repeat-x #ffffff;
  margin: 0px;
}

H1 {
  color: #0080ff;
  font: bold 20px Verdana, sans-serif;
  margin: 70px 0 40px 0;
}

H2 {
  color: #505050;
  font: bold 12px Verdana, sans-serif;
  margin: 15px 0 15px 0;
  padding: 5px;
  background: #EBF5FF;
  border: 1px solid #3299ff;
}

a {
  font-family: Verdana, sans-serif;
  color: #595959;
  font-style: sans-serif;
  font-size: 10pt;
}

a:hover {
  color: #404040;
}

.links {
  font-family: Verdana, sans-serif;
  color: #7B7B7B;
  font-style: sans-serif;
  font-size: 10pt;
  margin: 40px 0 0 0;
}

.logo {
  margin: 0 70px 0 0;
}

.stacktrace {
  padding: 0 5px 0 20px;
  background: url(<%=context%>/icons/page_text.gif ) no-repeat scroll 0%;
  margin: 10px 0 0 0;
}

.back {
  padding: 0 5px 0 20px;
  background: url(<%=context%>/icons/back.png ) no-repeat scroll 0%;
  margin: 10px 0;
}

.change {
  padding: 0 5px 0 20px;
  background: url(<%=context%>/icons/user_go.png ) no-repeat scroll 0%;
}

#stackTrace {
  border: 1px solid #999999;
  padding: 3px;
  margin: 15px 0;
  width: 700px;
  height: 700px;
  overflow: auto;
}
#requestDump {
  border: 1px solid #999999;
  padding: 3px;
  margin: 15px 0;
  width: 700px;
  height: 700px;
  overflow: auto;
}
-->
  </style>
  <script language="javascript" type="text/javascript">
    function toggleError(id) {
      var style = document.getElementById(id).style;
      if ("block" == style.display) {
        style.display = "none";
        document.getElementById(id + "Off").style.display = "inline";
        document.getElementById(id + "On").style.display = "none";
      } else {
        style.display = "block";
        document.getElementById(id + "Off").style.display = "none";
        document.getElementById(id + "On").style.display = "inline";
      }
    }
  </script>
</head>
<body>
<table border="0" width="75%" cellpadding="0" cellspacing="0" align="center">
  <tr>
    <td width="280" align="right" valign="top">
      <div class="logo">
        <img src="<%=context%>/img/logo_error.gif" alt="">
      </div>
    </td>
    <td>

      <h1>Page Not Found</h1>

      <div class="links">
        <div class="back"><a href="<%=context%>/">back</a>
        </div>
        <div class="change"><a href="<%=context%>/logout">change username</a>
        </div>
      </div>
    </td>
  </tr>
</table>

</body>
</html>

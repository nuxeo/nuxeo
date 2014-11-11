<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page language="java" %>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.plugins.AnonymousAuthenticator"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
  String context = request.getContextPath();
%>
<html>
<fmt:setBundle basename="messages" var="messages"/>
<head>
  <title><fmt:message bundle="${messages}" key="label.errorPage.pageNotFound.headerTitle" /></title>
  <style type="text/css">
<!--
body { background: url("<%=context%>/img/error_pages/page_background.gif") repeat scroll 0 0 transparent;
  color: #999;
  font: normal 100%/1.5 "Lucida Grande", Arial, Verdana, sans-serif;
  margin: 0;
  text-align: center }

.container {  margin: 2em auto;
  text-align: center;
  width: 70% }

h1 { color: #000;
  font-size: 150%;
  margin: 3.5em 0 .5em 0 }

h2 { color: #b20000;
  font-size: 110%;
  margin: 1em }

h1, h2 { font-weight: bold }

p { max-width: 600px; margin: .4em auto }

a.block { background: url("<%=context%>/img/error_pages/refresh.png") no-repeat scroll center 10px #fff;
  border: 1px solid #ddd;
  border-radius: 5px;
  color: #00729c;
  display: inline-block;
  font-weight: bold;
  margin: .4em;
  padding: 3em .5em .8em;
  text-align: center;
  text-decoration: none;
  vertical-align: top;
  width: 7em }
a.block:hover { background-color: #e9f1f4; border-color: #e9f1f4; color: #000 }
a.block.back { background-image: url("<%=context%>/img/error_pages/back.png") }
a.block.stack { background-image: url("<%=context%>/img/error_pages/show.png") }
a.block.dump { background-image: url("<%=context%>/img/error_pages/view.png") }

.block img { display: block;
    margin: 0 auto }

.links { margin: 2em 0 0 0 }
.links span { display: inline-block;
  font-size: 85% }
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

  <div class="container">
    <h1><fmt:message bundle="${messages}" key="label.errorPage.pageNotFound.title" /></h1>
    <p><fmt:message bundle="${messages}" key="label.errorPage.pageNotFound.description" /></p>

    <div class="links">
      <a class="block back" href="<%=context %>/">
        <span><fmt:message bundle="${messages}" key="label.errorPage.goBack" /></span>
      </a>
      <a class="block change" href="<%=context%>/logout">
        <span><fmt:message bundle="${messages}" key="label.errorPage.changeUsername" /></span>
      </a>
    </div>
  </div>

</body>
</html>
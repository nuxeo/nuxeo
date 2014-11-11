<!DOCTYPE html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@page import="org.nuxeo.wizard.context.Context"%>
<%@page import="org.nuxeo.wizard.context.ParamCollector"%>
<%@page import="org.nuxeo.wizard.nav.SimpleNavigationHandler"%>
<%@page import="org.nuxeo.wizard.nav.Page"%>
<%@page import="org.nuxeo.wizard.download.PackageDownloader"%>
<%@page import="org.nuxeo.wizard.download.DownloadablePackageOptions"%>
<%@page import="org.nuxeo.wizard.download.DownloadPackage"%>
<%@page import="java.util.ResourceBundle"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<fmt:setBundle basename="messages" />
<%
// Global rendering context init
String contextPath = request.getContextPath();
Context ctx = Context.instance(request);
Page currentPage = (Page) request.getAttribute("currentPage");
ParamCollector collector = ctx.getCollector();
SimpleNavigationHandler nav = SimpleNavigationHandler.instance();
%>

<html>

<head>
<title><fmt:message key="label.nuxeo.wizard" /></title>
<link rel="shortcut icon" href="<%=contextPath%>/images/favicon.ico"/>
<link rel="icon" href="<%=contextPath%>/images/favicon.png"/>
<link rel="stylesheet" href="<%=contextPath%>/css/nuxeo.css" type="text/css" media="screen" charset="utf-8" />
<script src="<%=contextPath%>/scripts/jquery-1.4.3.min.js"></script>
<script>
function navigateTo(page) {
  window.location.href='<%=contextPath%>/' + page;
}
function showError(id) {
  alert(id);
}

<% if (ctx.isBrowserInternetAccessChecked()) {%>
function hasBrowserInternetAccess() {
  return <%=ctx.hasBrowserInternetAccess()%>;
}
<%}%>

function showIframeIfPossible() {
  if (hasBrowserInternetAccess()) {
    $("#connectBannerIframe").css("visibility","visible");
  }
}
</script>
</head>

<body>

<div id="topbar">
<div class="container">
 <a href="<%=contextPath%>"><img src="<%=contextPath%>/images/nuxeo.png" height="20px" border="0"/></a>
</div>
</div>
<div style="text-align:center;">
<div id="wizardFrame" class="container">

<table width="100%" cellpadding="0" cellspacing="0">
  <tr valign="top" align="left">
    <td class="mainBlock">


<table width="100%">
<tr>
<td class="leftCell">

<%for (Page item : nav.getPages()) {

    if (item.isVisibleInNavigationMenu()) {
%>

<div
  class="navItem <%=currentPage.getAction().equals(item.getAction()) ? "navItemSelected" : "" %>"
>
<% if (item.hasBeenNavigatedBefore()) { %>
   <A href="#" onclick="navigateTo('<%=item.getAction()%>')" class="checked"> <fmt:message key="<%=item.getLabelKey()%>"/> </A>
<% } else { %>
  <fmt:message key="<%=item.getLabelKey()%>"/>
<%} %>
</div>

<% }
} %>

</td>
<td class="mainCell">

<!--
<% if (currentPage.getProgress()>=0) { %>
<table width="100%" class="progressbar">
<tr>
<td colspan="2" style="font-style:italic;font-color:#555555;text-align:center"><fmt:message key="label.nuxeo.wizard.progress" /></td>
</tr>
<tr style="border-style:solid;border-width:1px;border-color:#CCCCCC">
<td width="<%=currentPage.getProgress()%>%" style="background-color:#2888f8;padding:0px;margin:0px">&nbsp;</td>
<td width="<%=100-currentPage.getProgress()%>%" style="background-color:#DDDDDD;padding:0px;margin:0px"></td>
</tr>
</table>
<%}%>
 -->

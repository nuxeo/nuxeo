<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!-- Nuxeo Enterprise Platform, svn $Revision: 22925 $ -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%
    String productName = Framework.getProperty("org.nuxeo.dam.product.name");
    String productVersion = Framework.getProperty("org.nuxeo.dam.product.version");
%>
<html>

<fmt:setBundle basename="messages" var="messages" />

<head>
<title><%=productName%></title>
<link rel="icon" type="image/png" href="/nuxeo/icons/favicon.png" />
<style type="text/css">
<!--
 body {
  font: normal 11px "Lucida Grande", sans-serif;
  background: url(/nuxeo/img/login2_bg.jpg) 0 0 no-repeat #fff;
  color: #343434;
  }

.topBar {
  background:#212325 url(/nuxeo/img/banner_bground.png) repeat-x scroll left top;
  width:100%;
  height:30px;
  border:0;
  }

.topBar img {
  margin-left:62px;
  }

table.loginForm {
  border-spacing:3px;
  padding:3px;
  }

.leftColumn {
  width:300px;
  }

H1 {
  color:#343434;
  font:bold 14px "Lucida Grande", sans-serif;
  padding:0;
  margin:2px 0 15px 0;
  border-bottom:1px dotted #8B8B8B;
  }

H2 {
  color:#999;
  font:bold 10px "Lucida Grande", sans-serif;
  padding:0;
  margin:0 0 0 0;
  }

.extensionButtons {
  padding:0;
  margin:0 0 0 0;
  }

.linkButtons {
  padding:0;
  margin:0 0 0 0;
  }

.login {
  background:#fff;
  opacity:0.8;
  filter : alpha(opacity=80);
  border: 1px solid #4E9AE1;
  padding:20px 75px 5px 70px;
  width:250px;
  }

.login_label {
  font:bold 10px "Lucida Grande", sans-serif;
  color: #454545;
  margin:0 4px 0 0;
  width:70px;
  padding:5px 3px 0px 0px;
  vertical-align:top;
  }

.login_input {
  border:1px inset #454545;
  background: white;
  padding:3px;
  color: #454545;
  margin:0 10px 5px 0px;
  font:normal 10px "Lucida Grande", sans-serif;
  }

/* this class is a repeat because defined in nxthemes-setup.xml but
nxthemes css is not used in login.jsp */
.login_button {
  cursor:pointer;
  color: #454545;
  font-size: 10px;
  background: #CECFD1 url(/nuxeo/img/buttons.png) repeat-x scroll left top;
  border:1px solid #BFC5CB;
  padding: 2px 5px 2px 5px;
  margin: 5px 10px 10px 0;
  }

.login_button:hover {
  border:1px solid #92999E;
  color:#000000;
  }

.formTitle {
  margin:0 0 20px 0;
  text-align:center;
  color:#4a4a4a;
  font-size:14px;
  }

.footer {
  color:#c3c3c3;
  font-size: 9px;
  }

.loginLegal {
  padding: 0;
  margin: 0 0 10px 0;
  color:#c3c3c3;
  }

.loginLegal a, .loginLegal a:visited{
  color:#c3c3c3;
  }
 
.loginLegal a:hover{
  color:#d9d9d9;
  }

.version {
  padding-right:50px;
  }


.labelCorp {
  margin:0;
  width:400px;
  padding-top:0px;
  text-align:right;
  }

.labelCorp ul{
  margin:0;
  padding:0 42px 0 0;
  text-align:right;
  }

.labelCorp li {
  margin:0;
  padding:0px 8px;
  list-style:none;
  float:right;
  }


.labelCorp a {
  text-decoration:none;
  color:#9a9a9a;
  font:normal 9px "Lucida Grande", sans-serif;
  padding-top:0px;
  }

.labelCorp a:hover {
  text-decoration:underline;
  }

.block_container {
  margin-right:50px;
  border:none;
  height:500px;
  width:350px;
  overflow:auto;
  background-color:#ffffff;
  opacity:0.8;
  filter : alpha(opacity=80);
  }

.errorMessage {
  color:#000;
  font:bold 10px "Lucida Grande", sans-serif;
  border:1px solid #666;
  background: url(/nuxeo/icons/warning.gif) 2px 3px no-repeat #FFCC33;
  margin-bottom:12px;
  display:block;
  padding:5px 5px 5px 23px;
  text-align: center;
  }
-->

</style>
</head>

<body style="margin: 0; text-align: center;">

<table cellspacing="0" cellpadding="0" border="0" width="100%"
  height="100%">
  <tbody>
    <tr class="topBar">
      <td colspan="2" style="height:30px;">
        <div>
          <img width="125" height="30" alt="Nuxeo DAM" src="/nuxeo/img/dam_logo.png"/>
        </div>
      </td>
      <td align="right" class="leftColumn">
        <div class="labelCorp">
           <ul>
             <li>
               <a href="http://www.nuxeo.com/en">
                 <fmt:message bundle="${messages}" key="label.login.visitNuxeoCom" />
               </a>
             </li>
             <li>
               <a href="http://www.nuxeo.com/en/services/support">
                 <fmt:message bundle="${messages}" key="label.login.getSupport" />
               </a>
             </li>
             <li>
               <a href="http://www.nuxeo.org/sections/community/">
                 <fmt:message bundle="${messages}" key="label.login.joinTheCommunity" />
               </a>
             </li>
           </ul>
           <div style="clear:both;" />
          </div>
       </td>
     </tr>
     <tr>
       <td align="center" colspan="2">
         <form method="post" action="nxstartup.faces">
           <!-- To prevent caching -->
          <%
		        response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
		        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
		        response.setDateHeader("Expires", -1); // Prevents caching at the proxy server
          %>
      			<div class="login">
				      <table>
				        <tr>
				          <td class="login_label"><label for="username"> <fmt:message
				            bundle="${messages}" key="label.login.username" /> </label></td>
				          <td><input class="login_input" type="text"
				            name="user_name" id="username" size="22"></td>
				        </tr>
				        <tr>
				          <td class="login_label"><label for="password"> <fmt:message
				            bundle="${messages}" key="label.login.password" /> </label></td>
				          <td><input class="login_input" type="password"
				            name="user_password" id="password" size="22"></td>
				        </tr>
				        <tr>
				          <td class="login_label">
				            <label for="language">
				              <fmt:message bundle="${messages}" key="label.login.language" /> 
				            </label>
				          </td>
				          <td>
				            <select class="login_input" name="language" id="language">
				              <option value="en" selected>English</option>
				              <option value="fr">français</option>
				            </select>
				          </td>
				        </tr>
				        <tr>
				          <td></td>
				          <td><input type="hidden" name="form_submitted_marker"
				            id="form_submitted_marker"> <input
				            class="login_button" type="submit" name="Submit"
				            value="<fmt:message bundle="${messages}" key="label.login.logIn" />"></td>
				        </tr>
				        <tr>
				          <td></td>
				          <td><c:if test="${param.loginFailed}">
				            <div class="errorMessage"><fmt:message
				              bundle="${messages}"
				              key="label.login.invalidUsernameOrPassword" /></div>
					          </c:if> <c:if test="${param.loginMissing}">
					            <div class="errorMessage"><fmt:message
					              bundle="${messages}" key="label.login.missingUsername" /></div>
					          </c:if>
				          </td>
				        </tr>
				      </table>
				    </div>
     		  </form>
        </td>
        <td class="news_container" align="right" valign="center">
        <iframe class="block_container" style="visibility:hidden"
          onload="javascript:this.style.visibility='visible';"
          src="http://www.nuxeo.com/layout/set/dam_newslist/"></iframe>
      </td>
        </td>
      </tr>
      <tr class="footer">
        <td valign="bottom" style="padding-left:70px;">
          <div class="loginLegal">
            Photography: &#169; <a href="mailto:l.viatour@mm.be">Luc Viatour</a> GFDL/CC / <a href="www.lucnix.be/">www.lucnix.be</a> | Colors: &#169; Michael Yucha / <a href="http://www.flickr.com/photos/greenwenvy/">flickR</a> 
          </div>
        </td>
	      <td align="center" valign="bottom">
		      <div class="loginLegal"><fmt:message bundle="${messages}"
		        key="label.login.copyright" /></div>
		    </td>
	      <td align="right" class="version" valign="bottom">
	      	<div class="loginLegal"><%=productName%> &nbsp; <%=productVersion%></div>
	      </td>
      </tr>
    </tbody>
  </table>
</body>
</html>


<html>
<head>
  <title>
    <@block name="title">Nuxeo Connect</@block>
  </title>
  <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">

  <!--link rel="stylesheet" href="${skinPath}/css/webengine.css" type="text/css" media="screen" charset="utf-8"-->
  <link rel="shortcut icon" href="${skinPath}/images/favicon.png" />
  <link rel="stylesheet" href="${skinPath}/css/studio_style.css" type="text/css" media="screen" charset="utf-8">

  <@block name="stylesheets" />

   <script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
   <script type="text/javascript" src="${skinPath}/script/jquery/cookie.js"></script>

   <@block name="header_scripts" />

</head>

<body>

<table width="100%" cellpadding="0" cellspacing="0">
  <tr valign="middle">
    <td class="header">
      <@block name="header">
        <div class="logo">
          <a href="${Root.path}"><img src="${skinPath}/images/logo_connect_white.png" height="28px" border="0"/></a>
        </div>
        <div class="login">
           <#include "nxlogin.ftl">
           <!--input type="text" size="15" value="login">
           <input type="text" size="15" value="password">
           <input class="button" type="submit" value="ok"-->
        </div>
        <div style="clear:both;"></div>
      </@block>
    </td>
  </tr>
  <tr valign="top" align="left">
    <td>
      <@block name="middle">
      <table width="100%" cellpadding="0" cellspacing="0">
        <tr valign="top" align="left">
          <td width="15%" style="padding:20px 10px 20px 20px; border-right:1px solid #d2d2d2">
          <@block name="left">

          <#include "nav.ftl">

          </@block>
          </td>
          <td style="padding:10px 20px 20px 10px;">
          <@block name="right">
            Content
          </@block>
          </td>
        </tr>
      </table>
      </@block>
    </td>
  </tr>
  <tr valign="middle" align="center">
    <td class="footer">
      <@block name="footer">
        <div class="copyrights">Copyright &#169; 2009 Nuxeo and its respective authors. </div>
        <div class="links">
          <span>visit <a href="http://www.nuxeo.com/en">nuxeo.com</a></span>
          <span>get <a href="http://www.nuxeo.com/en/services/support">support</a>!</span>
          <span>join our <a href="http://www.nuxeo.org/sections/community/">community</a></span>
        </div>
        <div style="clear:both;"></div>
      </@block>
    </td>
  </tr>
</table>


</body>
</html>
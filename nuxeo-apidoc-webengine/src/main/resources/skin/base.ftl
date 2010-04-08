<html>
<head>
  <title>
    <@block name="title">Nuxeo Connect</@block>
  </title>
  <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">

  <link rel="shortcut icon" href="${skinPath}/images/favicon.png" />
  <link rel="stylesheet" href="${skinPath}/css/apidoc_style.css" type="text/css" media="screen" charset="utf-8" />
  <link rel="stylesheet" href="${skinPath}/css/code.css" type="text/css" media="screen" charset="utf-8" />

  <@block name="stylesheets" />

   <script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
   <script type="text/javascript" src="${skinPath}/script/jquery/cookie.js"></script>
   <script type="text/javascript" src="${skinPath}/script/highlight.js"></script>
   <script type="text/javascript" src="${skinPath}/script/java.js"></script>
   <script type="text/javascript" src="${skinPath}/script/html-xml.js"></script>
   <script type="text/javascript" src="${skinPath}/script/manifest.js"></script>

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
          <td width="20%" style="padding:20px 10px 20px 20px; border-right:1px solid #d2d2d2">
          <@block name="left">

          <#include "nav.ftl">

          </@block>
          </td>
          <td style="padding:10px 20px 20px 10px;">
     <#if enableDocumentationView>
            <table width="100% class="tabs" id="tabbox">
            <tr>

            <td width="33%">
            <div class="tabs
            <#if "docView"!=selectedTab && "aggView"!=selectedTab>
        tabselected
            </#if>
            ">
             <A href="${This.path}">Introspection view</A>
            </div> </td>

            <td width="33%" >
            <div class="tabs
            <#if "docView"==selectedTab>
            tabselected
            </#if>
            "> <A href="${This.path}/doc">Documentation view</A></div></td>

            <td width="33%" >
            <div class="tabs
            <#if "aggView"==selectedTab>
            tabselected
            </#if>
            "> <A href="${This.path}/aggView">Aggregated view</A></div></td>

            </tr>
            </table>
       </#if>
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


<script type="text/javascript">

    hljs.initHighlightingOnLoad();

    // toggle code viewer
    $(".resourceToggle").click(function() {
     $(this).next().toggleClass('hiddenResource');
    });

    // toggle title bars
    $(".blocTitle").click(function() {
     var toFold=$(this).parent().find(".foldablePannel").get(0);
     $(toFold).toggle("fold",{horizFirst: true },10);
    });

    var lastDisplayedDoc;
    function showAddDoc(docId) {
      if (lastDisplayedDoc) {
       if (lastDisplayedDoc!=docId) {
         $('#' + lastDisplayedDoc).toggle("fold",{horizFirst: true},1000);
       }
      }
      $('#' + docId).toggle("fold",{horizFirst: true},1000);
      lastDisplayedDoc=docId;
    }

</script>

</body>
</html>

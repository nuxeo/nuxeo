<html>
<head>
  <title>
    <@block name="title">Nuxeo Platform Explorer</@block>
  </title>
  <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">

  <link rel="shortcut icon" href="${skinPath}/images/favicon.png" />
  <link rel="stylesheet" href="${skinPath}/css/apidoc_style.css" type="text/css" media="screen" charset="utf-8" />
  <link rel="stylesheet" href="${skinPath}/css/code.css" type="text/css" media="screen" charset="utf-8" />
  <link rel="stylesheet" href="${skinPath}/script/jquery//treeview/jquery.treeview.css" type="text/css" media="screen" charset="utf-8">

  <@block name="stylesheets" />
   <script type="text/javascript">
     var skinPath = '${skinPath}';
   </script>
   <script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
   <script type="text/javascript" src="${skinPath}/script/jquery/cookie.js"></script>
   <script type="text/javascript" src="${skinPath}/script/highlight.js"></script>
   <script type="text/javascript" src="${skinPath}/script/java.js"></script>
   <script type="text/javascript" src="${skinPath}/script/html-xml.js"></script>
   <script type="text/javascript" src="${skinPath}/script/manifest.js"></script>

   <script type="text/javascript" src="${skinPath}/script/jquery//treeview/jquery.treeview.js"></script>
   <script type="text/javascript" src="${skinPath}/script/jquery//treeview/jquery.treeview.async.js"></script>
   <script type="text/javascript" src="${skinPath}/script/quickEditor.js"></script>
   <script type="text/javascript" src="${skinPath}/script/jquery.highlight-3.js"></script>

   <@block name="header_scripts" />

</head>

<body>

<table width="100%" cellpadding="0" cellspacing="0">
<#if !Root.isEmbeddedMode()>
  <tr valign="middle">
    <td class="pageheader">
      <@block name="header">
        <div class="logo">
          <a href="${Root.path}"><img src="${skinPath}/images/nuxeo_white_logo.png" width="89" height="25px" border="0"/></a><span>Nuxeo Platform Explorer</span>
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
</#if>
  <tr valign="top" align="left">
    <td>
      <@block name="middle">
      <table width="100%" cellpadding="0" cellspacing="0">
        <tr valign="top" align="left">
        <#if !hideNav>

          <td width="20%" style="padding:10px; border-right:1px solid #d2d2d2">
          <@block name="left">

          <#include "nav.ftl">

          </@block>
          </td>
        </#if>
          <td style="padding:10px;">
     <#if enableDocumentationView && !Root.isEmbeddedMode() >


     <div class="tabsbar">
       <ul>
         <li <#if selectedTab=="defView">class="selected"</#if> >
                <a href="${This.path}/">View</a>
         </li>
         <li <#if selectedTab=="docView">class="selected"</#if> >
                <a href="${This.path}/doc">Documentation view</a>
         </li>
       </ul>
     </div>

     <div style="clear:both;"></div>

       </#if>
         <div class="tabscontent">
          <@block name="right">
            Content
          </@block>
          </div>
          </td>
        </tr>
      </table>
      </@block>
    </td>
  </tr>
  <#if !Root.isEmbeddedMode()>
  <tr valign="middle" align="center">
    <td class="footer">
      <@block name="footer">
        <div class="copyrights">Copyright &#169; 2009-2011 Nuxeo and its respective authors. </div>
        <div class="links">
          <span>visit <a href="http://www.nuxeo.com/en">nuxeo.com</a></span>
          <span>get <a href="http://www.nuxeo.com/en/services/support">support</a>!</span>
          <span>join our <a href="http://www.nuxeo.org/sections/community/">community</a></span>
        </div>
        <div style="clear:both;"></div>
      </@block>

    </td>
  </tr>
  </#if>
</table>


<script type="text/javascript">

    hljs.initHighlightingOnLoad();

    // toggle code viewer
    $(".resourceToggle").click(function() {
     $(this).next().toggleClass('hiddenResource');
     $(this).toggleClass('resourceToggle');
     $(this).toggleClass('resourceToggleUp');
    });

    // toggle title bars
    //$(".blocTitle").click(function() {
    // var toFold=$(this).parent().find(".foldablePanel").get(0);
    // $(toFold).toggle("fold",{horizFirst: true },10);
    //});

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

<@block name="footer_scripts" />

</body>
</html>

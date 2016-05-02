<!DOCTYPE html>
<html>
<head>
  <title>
    <@block name="title">Nuxeo Platform Explorer</@block>
  </title>
  <meta http-equiv="Content-Type" charset="UTF-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
  <meta name="author" content="Nuxeo">
  <meta name="description" content="Nuxeo Platform Explorer">

  <link rel="shortcut icon" href="${skinPath}/images/favicon.png" />
  <link rel="stylesheet" href="${skinPath}/css/apidoc_style.css" media="screen" charset="utf-8" />
  <link rel="stylesheet" href="${skinPath}/css/code.css" media="screen" charset="utf-8" />
  <link rel="stylesheet" href="${skinPath}/script/jquery//treeview/jquery.treeview.css" media="screen" charset="utf-8">
  <link rel="stylesheet" href="//fonts.googleapis.com/css?family=PT+Sans+Caption:400,700">

  <@block name="stylesheets" />
   <script type="text/javascript">
     var skinPath = '${skinPath}';
   </script>
   <script src="//code.jquery.com/jquery-1.7.2.min.js"></script>
   <script src="${skinPath}/script/jquery/cookie.js"></script>
   <script src="${skinPath}/script/highlight.js"></script>
   <script src="${skinPath}/script/java.js"></script>
   <script src="${skinPath}/script/html-xml.js"></script>
   <script src="${skinPath}/script/manifest.js"></script>

   <script src="${skinPath}/script/jquery//treeview/jquery.treeview.js"></script>
   <script src="${skinPath}/script/jquery//treeview/jquery.treeview.async.js"></script>
   <script src="${skinPath}/script/quickEditor.js"></script>
   <script src="${skinPath}/script/jquery.highlight-3.js"></script>
   <@block name="header_scripts" />

</head>

<body>
  <#if !Root.isEmbeddedMode()>
  <header role="banner">
    <@block name="header">
    <h1>
      <span class="nuxeo">nuxeo</span><span class="slash">/</span><span class="doctitle">Platform Explorer</span>
    </h1>
    <nav role="complementary">
      <div class="login">
         <#include "nxlogin.ftl">
         <!--input type="text" size="15" value="login">
         <input type="text" size="15" value="password">
         <input class="button" type="submit" value="ok"-->
      </div>
     </nav>
     </@block>
  </header>
  </#if>
  <div class="container">
    <table class="content">
      <tr>
      <@block name="middle">
        <#if !hideNav>
        <td class="leftblock">
          <nav role="navigation">
          <@block name="left">
          <#include "nav.ftl">
          </@block>
          </nav>
        </td>
        </#if>

        <td class="rightblock">
         <section>
           <article role="contentinfo">
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
             <#if enableDocumentationView?? && !enableDocumentationView && !Root.isEmbeddedMode()>
               <div class="tabsbutton">
                 <a href="${This.path}/doc">Add custom Documentation</a>
               </div>
             </#if>
             <div class="tabscontent">
             <#if !enableDocumentationView && !Root.isEmbeddedMode() >
               <a href="${This.path}/doc">Add custom Documentation</a>
             </#if>
             <@block name="right">
               Content
             </@block>
             </div>
           </article>
         </section>
       </td>
      </@block>
      </tr>
    </table>
  </div>
<script type="text/javascript">

    hljs.initHighlightingOnLoad();

    // toggle code viewer
    $(".resourceToggle").click(function() {
     $(this).next().toggle();
     $(this).toggleClass('resourceToggle');
     $(this).toggleClass('resourceToggleDown');
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
         $('#' + lastDisplayedDoc).toggle();
       }
      }
      $('#' + docId).toggle();
      lastDisplayedDoc=docId;
    }


</script>

<@block name="footer_scripts" />
<#if !Root.isEmbeddedMode()>
<script type="text/javascript" src="//www.nuxeo.com/wp-content/themes/nuxeo.com_wp/js/xnav_get.js" charset="utf-8"></script>
</#if>
</body>
</html>

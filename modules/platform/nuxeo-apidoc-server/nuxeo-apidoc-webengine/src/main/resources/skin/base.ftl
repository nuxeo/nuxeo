<!DOCTYPE html>
<html lang="en" xml:lang="en" xmlns="http://www.w3.org/1999/xhtml">
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
  <link rel="stylesheet" href="${skinPath}/css/jquery.magnific.min.css" media="screen" charset="utf-8">
  <link rel="stylesheet" href="//fonts.googleapis.com/css?family=PT+Sans+Caption:400,700">

  <@block name="stylesheets" />
   <script type="text/javascript">
     var skinPath = '${skinPath}';

     function fixJavaDocPaths(javaDocDiv, javaDocBaseUrl) {
       return function(data) {
         var $data = $(data);
         // URLs come with a local href; fix them to target javadoc website instead
         $data.find('a[href^="../../../../../"]').each(function() {
           this.href = $(this).attr('href').replace('../../../../../', javaDocBaseUrl + '/javadoc/');
         });

         $(javaDocDiv).html($data);
       };
     }
   </script>
   <script src="//code.jquery.com/jquery-1.7.2.min.js"></script>
   <script src="${skinPath}/script/jquery/cookie.js"></script>
   <script src="${skinPath}/script/highlight.js"></script>
   <script src="${skinPath}/script/java.js"></script>
   <script src="${skinPath}/script/html-xml.js"></script>
   <script src="${skinPath}/script/manifest.js"></script>

   <script src="${skinPath}/script/jquery//treeview/jquery.treeview.js"></script>
   <script src="${skinPath}/script/jquery//treeview/jquery.treeview.async.js"></script>
  <script src="${skinPath}/script/jquery.magnific.min.js"></script>
   <script src="${skinPath}/script/quickEditor.js"></script>
   <script src="${skinPath}/script/jquery.highlight-3.js"></script>
   <@block name="header_scripts" />

</head>

<body>
  <#if !Root.isEmbeddedMode()>
  <header role="banner">
    <@block name="header">
    <div class="top-banner">
      <a href="/nuxeo/site/distribution">
        <img src="${skinPath}/images/nuxeo.png">
        <span>Platform Explorer</span>
      </a>
      <span>
        / ${Root.currentDistribution.name} ${Root.currentDistribution.version}
      </span>
      <div class="login">
        <#include "nxlogin.ftl">
      </div>
    </div>
    </@block>

    <#if !hideNav>
      <nav role="navigation">
      <@block name="left">
        <#include "nav.ftl">
      </@block>
      </nav>
    </#if>
  </header>
  </#if>

  <div class="container content">
      <@block name="middle">
         <section>
           <article role="contentinfo">
             <#if false && onArtifact?? && Root.canAddDocumentation()>
               <div class="tabsbutton">
                 <a class="button" href="${This.path}/doc">Manage Documentation</a>
               </div>
             </#if>
             <#if onArtifact??>
               <@googleSearchFrame This.searchCriterion />
             </#if>
             <@block name="right">
               Content
             </@block>
           </article>
         </section>
      </@block>
  </div>
<script type="text/javascript">

    hljs.initHighlightingOnLoad();

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
<@ga />
</body>
</html>
<#macro ga>
<script>
  !function() {
    if (window.location.host.match(/localhost/)) {
      // Skip analytics tracking on localhost
      return;
    }

    var analytics = window.analytics = window.analytics || [];
    if (!analytics.initialize) {
      if (analytics.invoked) {
        window.console && console.error && console.error("Segment snippet included twice.");
      } else {
        analytics.invoked = !0;
        analytics.methods = ["trackSubmit", "trackClick", "trackLink", "trackForm", "pageview", "identify", "reset", "group", "track", "ready", "alias", "page", "once", "off", "on"];
        analytics.factory = function(t) {
          return function() {
            var e = Array.prototype.slice.call(arguments);
            e.unshift(t);
            analytics.push(e);
            return analytics
          }
        };
        for (var t = 0; t < analytics.methods.length; t++) {
          var e = analytics.methods[t];
          analytics[e] = analytics.factory(e)
        }
        analytics.load = function(t) {
          var e = document.createElement("script");
          e.type = "text/javascript";
          e.async = !0;
          e.src = ("https:" === document.location.protocol ? "https://" : "http://") + "cdn.segment.com/analytics.js/v1/" + t + "/analytics.min.js";
          var n = document.getElementsByTagName("script")[0];
          n.parentNode.insertBefore(e, n)
        };
        analytics.SNIPPET_VERSION = "3.1.0";
        analytics.load("4qquvje3fv");
        analytics.page()
      }
    }
  }();
</script>
</#macro>

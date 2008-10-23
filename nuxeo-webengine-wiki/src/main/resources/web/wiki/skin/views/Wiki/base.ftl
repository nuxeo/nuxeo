<html>
<#import "common/util.ftl" as base/>
<#import "navigation/tree.ftl" as nav/>

<head>
    <title>${Root.document.title} :: ${This.document.title}</title>
    
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
    
<link rel="stylesheet" href="/nuxeo/site/files/resources/css/webengine.css" type="text/css" media="screen" charset="utf-8">
<link rel="stylesheet" href="/nuxeo/site/files/resources/css/wiki.css" type="text/css" media="screen" charset="utf-8">
<link rel="stylesheet" href="/nuxeo/site/files/resources/script/jquery/ui/themes/flora/flora.all.css" type="text/css" media="screen" title="Flora (Default)">
<link rel="stylesheet" type="text/css" href="/nuxeo/site/files/resources/script/markitup/skins/markitup/style.css" />
<link rel="stylesheet" type="text/css" href="/nuxeo/site/files/resources/script/markitup/sets/wiki/style.css" />


<script src="/nuxeo/site/files/resources/script/jquery/jquery.js"></script>
  <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/ui/ui.base.js"></script>
  <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/ui/ui.tabs.js"></script>
  <script type="text/javascript" src="/nuxeo/site/files/resources/script/base64.js"></script>
  <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/cookie.js"></script>
  <script type="text/javascript" src="/nuxeo/site/files/resources/script/nxlogin.js"></script>
  
  <link rel="shortcut icon" href="/nuxeo/site/files/resources/image/favicon.gif" />
  
  <base href="${This.urlPath}">

<script>  
$(document).ready(function(){

$('#q').focus(function() {
  if (this.value == "Search") {
     this.value = ""
  }
})

})

</script>

</head>

<body>

<div id="wrap">
    <div id="header">
       <div class="searchBox">
      <form action="${Root.urlPath}@@search" method="get" accept-charset="utf-8">
        <input type="search" name="q" id="q" autosave="${Request.localName}" results="5" value="Search">
        <input type="hidden" name="p" value="${Root.repositoryPath}">
      </form>
     </div>  
       <h1><a href="${Root.urlPath}">${Root.document.title}</a></h1>
    </div>
    <div id="main-wrapper">
      <div id="main">
        <div class="main-content">
          <@block name="content">
          ##This is the content block##
          </@block>
        </div>  
      </div>

      <div id="sidebar">
          <#include "includes/sidebar.ftl"/>
          <@nav.navtree rootdoc=Root.document />
      </div>
    </div>
    <div id="footer">
       <p>Last modified by ${Document.dublincore.creator} @ ${Document.dublincore.modified?datetime}</p>
       <p>&copy; 2000-2008 <a href="http://www.nuxeo.com/en/">Nuxeo</a>.</p>
    </div>
    
</div>

</body>
</html>
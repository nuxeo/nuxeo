<html>
<head>
  <title>
    <@block name="title">WebEngine</@block>
  </title>
  <meta http-equiv="Content-Type" content="text/html;charset=UTF-8"/>

  <link rel="stylesheet" href="${skinPath}/css/webengine.css" type="text/css" media="screen" charset="utf-8">
  <link rel="stylesheet" href="${skinPath}/css/wiki.css" type="text/css" media="screen" charset="utf-8">
  <link rel="stylesheet" href="${skinPath}/script/jquery/ui/themes/flora/flora.all.css" type="text/css" media="screen" title="Flora (Default)">
  <link rel="stylesheet" type="text/css" href="${skinPath}/script/markitup/skins/markitup/style.css" />
  <link rel="stylesheet" type="text/css" href="${skinPath}/script/markitup/sets/wiki/style.css" />
  <link rel="shortcut icon" href="${skinPath}/image/favicon.gif" />

  <@block name="stylesheets" />
  <script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.base.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.tabs.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery/cookie.js"></script>
  <script type="text/javascript" src="${skinPath}/script/json.js"></script>
  <script type="text/javascript" src="${skinPath}/script/webengine.js"></script>
  <#if Document??> <#-- we are in a document context: search is enabled -->
    <script>
      $(document).ready(function(){
        $('#query').focus(function() {
          if (this.value == "Search") {
            this.value = "";
          }
        });
      });
    </script>
  </#if>
  <@block name="header_scripts" />
</head>

<body>

<div id="wrap">
    <div id="header">
      <div class="webEngineRoot"><a href="${appPath}"><img src="${skinPath}/image/dots.png" width="16" height="16" alt=""/></a></div>
       <@block name="header">
       <#if Document??>
        <div class="searchBox">
         <form action="${This.path}/@search" method="get" accept-charset="utf-8">
           <input type="search" name="fullText" id="query" results="5" value="Search">
           <input type="hidden" name="orderBy" value="dc:modified">
         </form>
        </div>
        <h1><a href="${Context.modulePath}">${Root.document.title}</a></h1>
       </#if>
       </@block>
    </div>
    <div id="main-wrapper">
      <div id="main">
        <div class="main-content">
          <div id="message"><@block name="message">${Context.getProperty('msg')}</@block></div>
          <div id="content"><@block name="content" /></div>
        </div>
      </div>

      <div id="sidebar">

<!-- header -->
<div class="sideblock general">
<@block name="sidebar-header">
  <#include "common/nxlogin.ftl">
</@block>
</div>

<!-- toolbox -->
<@block name="toolbox">
<#if This??>
  <div class="sideblock contextual">
    <h3>Toolbox</h3>
    <div class="sideblock-content">
      <ul>
        <#list This.getLinks("TOOLBOX") as link>
        <#if link.id == "link_print">
          <li><a href="${link.getCode(This)}" target="_blank">${Context.getMessage(link.id)}</a></li>
        <#else>
          <li><a href="${link.getCode(This)}">${Context.getMessage(link.id)}</a></li>
        </#if>
        </#list>
      </ul>
    </div>
  </div>
</#if>
</@block>

<div class="sideblock general">
  <@block name="sidebar-footer">
    <h3>Help</h3>
    <ul>
    <#list This.getLinks("HELP") as link>
      <li><a href="${link.getCode(This)}">${Context.getMessage(link.id)}</a></li>
    </#list>
    </ul>
  </@block>
</div>

<!-- content -->
      </div>
    </div>
    <div id="footer">
       <@block name="footer">
       <p>&copy; 2000-2011 <a href="http://www.nuxeo.com/en/">Nuxeo</a>.</p>
       </@block>
    </div>

</div>

</body>
</html>

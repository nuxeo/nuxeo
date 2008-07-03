<html>

  <head>
    <title>
       <@block name="title">
       <#if Root??>WebEngine - ${Root.document['dc:title']}<#else>WebEngine</#if>
       </@block>
    </title>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
    <link rel="stylesheet" href="/nuxeo/site/files/resources/css/webengine.css" type="text/css" media="screen" charset="utf-8">
    <script src="/nuxeo/site/files/resources/script/jquery/jquery.js"></script>
    <link rel="stylesheet" href="/nuxeo/site/files/resources/script/jquery/ui/themes/flora/flora.all.css" type="text/css" media="screen" title="Flora (Default)">
    <link rel="shortcut icon" href="/nuxeo/site/files/resources/image/favicon.gif" />
    <@block name="stylesheets" />
    <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/ui/ui.base.js"></script>
    <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/ui/ui.tabs.js"></script>
    <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/cookie.js"></script>
    <script type="text/javascript" src="/nuxeo/site/files/resources/script/json.js"></script>
    <script type="text/javascript" src="/nuxeo/site/files/resources/script/webengine.js"></script>
    <#if Root??> <#-- we are in a document context: search is enabled -->
          <script>
           $(document).ready(function(){
             $('#q').focus(function() {
               if (this.value == "Search") {
                 this.value = ""
               }
             })
           })
          </script>
    </#if>
     <@block name="header_scripts" />
  </head>

<body>

<div id="wrap">
    <div id="header">
      <div class="webEngineRoot"><a href="${appPath}"><img src="/nuxeo/site/files/resources/image/dots.png" width="16" height="16" alt=""/></a></div>
       <@block name="header">
       <#if Root??>
        <div class="searchBox">
         <form action="${Root.urlPath}@@search" method="get" accept-charset="utf-8">
           <input type="search" name="q" id="q" results="5" value="Search">
           <input type="hidden" name="p" value="${Root.repositoryPath}">
         </form>
        </div>
        <h1><a href="${appPath}/${Root.document.name}">${Root.document.title}</a></h1>
       </#if>
       </@block>
    </div>
    <div id="main-wrapper">
      <div id="main">
        <div class="main-content">
          <div id="message"><@block name="message">${Request.getParameter('msg')}</@block></div>
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
        <#list This.getActions("TOOLBOX") as action>
        <#if action.id == "print">
          <li><a href="${This.urlPath}@@${action.id}" target="_blank">${message('action.' + action.id)}</a></li>
        <#else>
          <li><a href="${This.urlPath}@@${action.id}">${message('action.' + action.id)}</a></li>
        </#if>
        </#list>
      </ul>
    </div>
  </div>
</#if>
</@block>

<!-- content -->
<div class="sideblock general">
  <@block name="sidebar-content">
    <#if Context.principal.isAdministrator()>
    <h3>Administration</h3>
    <ul>
      <li><a href="${basePath}/admin">Admin board</a></li>    
      <li><a href="${basePath}/admin/users">User Management</a></li>
    </ul>
    </#if>
  </@block>
</div>
<!-- content -->
<div class="sideblock general">
  <@block name="sidebar-footer">
    <h3>Help</h3>
    <ul>
      <li><a href="${basePath}/docs/about.ftl">About</a></li>
      <li><a href="${basePath}/docs/index.ftl">Documentation</a></li>
    </ul>
  </@block>
</div>

      </div>
    </div>
    <div id="footer">
       <@block name="footer">
       <p>&copy; 2000-2008 <a href="http://www.nuxeo.com/en/">Nuxeo</a>.</p>
       </@block>
    </div>

</div>

</body>
</html>

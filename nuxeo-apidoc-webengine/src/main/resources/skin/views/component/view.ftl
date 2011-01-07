<@extends src="base.ftl">
<#setting url_escaping_charset="UTF-8">

<@block name="stylesheets"></@block>

<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.base.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.tabs.js"></script>
</@block>

<@block name="right">
<#include "/views/component/macros.ftl">

<h1>Component <span class="componentTitle">${component.id}</span>
  <a href="${Root.path}/${distId}/viewBundle/${component.bundle.id}" title="Go to parent bundle">
    <img src="${skinPath}/images/up.gif"/>
  </a>
</h1>

<@viewComponentDoc component This.getAssociatedDocuments()/>
<@viewComponentImpl component/>
<@viewComponentServices component/>
<@viewComponentExtensionPoints component/>
<@viewComponentContributions component/>

<h2> XML source </h2>
<pre><code>${component.xmlFileContent?html}</code></pre>

</@block>

</@extends>
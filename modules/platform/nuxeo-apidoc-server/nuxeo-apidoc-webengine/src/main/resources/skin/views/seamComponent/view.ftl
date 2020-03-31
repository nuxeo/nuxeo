<@extends src="base.ftl">
<#setting url_escaping_charset="UTF-8">

<@block name="stylesheets"></@block>

<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.base.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.tabs.js"></script>
</@block>

<@block name="right">

<h1> view Seam Component ${seamComponent.name}</h1>

<div class="tabscontent">
  <#include "/views/seamComponent/viewSimple.ftl">
</div>

</@block>

</@extends>
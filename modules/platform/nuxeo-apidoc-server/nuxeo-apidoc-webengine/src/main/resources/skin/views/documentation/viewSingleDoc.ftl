<@extends src="base.ftl">
<#setting url_escaping_charset="UTF-8">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1> view ${doc.title} </h1>

<div class="tabscontent">
  <@docContent doc/>
</div>

</@block>

</@extends>

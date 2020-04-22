<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">

<@block name="stylesheets"></@block>
<@block name="header_scripts"></@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1> View Contribution <span class="componentTitle">${nxItem.id}</span></h1>

<div class="tabscontent">
  <#include "/views/contribution/contributionMacros.ftl">
  <@viewContribution contributionWO=This />
</div>

</@block>

</@extends>

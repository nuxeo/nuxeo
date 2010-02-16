<@extends src="base.ftl">

<@block name="stylesheets">
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">

<h1> listing all deployed bundle groups </h1>
<ul>
<#list tree as bundleGroup>

  <#if bundleGroup.level==0>
  <li>
  </#if>
  <#if bundleGroup.level==1>
  <ul><li>
  </#if>

  <A href="${Root.path}/${distId}/viewBundleGroup/${bundleGroup.group.key}">${bundleGroup.group.name}</A><br/>
  <#if bundleGroup.level==1>
  </li></ul>
  </#if>
  <#if bundleGroup.level==1>
  </li>
  </#if>

</#list>
</ul>
</@block>

</@extends>
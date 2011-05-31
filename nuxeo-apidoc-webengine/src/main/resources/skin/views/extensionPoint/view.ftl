<@extends src="base.ftl">

<@block name="stylesheets"></@block>
<@block name="header_scripts"></@block>

<#if Root.isEmbeddedMode()>
  <#assign hideNav=true/>
</#if>

<@block name="right">

<h1>Extension point <span class="componentTitle">${extensionPoint.name}</span>
  of component ${extensionPoint.component.name?replace(".*\\.", "", "r")}
  <a href="${Root.path}/${distId}/viewComponent/${extensionPoint.component.name}" title="Go to parent component">
    <img src="${skinPath}/images/up.gif"/>
  </a>
</h1>

<h2>Documentation</h2>
${extensionPoint.documentationHtml}

<h2>Contributions</h2>
<ul>
  <#list extensionPoint.extensions as contrib>
  <li>
    <a href="${Root.path}/${distId}/viewContribution/${contrib.id}">${contrib.id}</a>
  </li>
  </#list>
</ul>

</@block>

</@extends>

<@extends src="base.ftl">

<#if Root.isEmbeddedMode()>
  <#assign hideNav=true/>
</#if>

<@block name="right">

<h1>Extension point <span class="componentTitle">${extensionPoint.name}</span>
  of component ${extensionPoint.component.name?replace(".*\\.", "", "r")}
</h1>

<div class="tabscontent">

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

</div>

</@block>

</@extends>

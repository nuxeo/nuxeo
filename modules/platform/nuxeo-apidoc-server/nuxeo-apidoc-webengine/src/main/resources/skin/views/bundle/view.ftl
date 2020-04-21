<@extends src="base.ftl">

<@block name="right">
<#include "/views/bundle/macros.ftl">
<#assign nestedLevel=0/>

<h1>Bundle <span class="componentTitle">${bundle.bundleId}<span></h1>

<div class="tabscontent">

  <@viewBundleArtifact bundleItem=This.nxArtifact/>

  <h2> MANIFEST </h2>
  <pre>
  ${bundle.manifest}
  </pre>

  <h2> Components </h2>
  <#if components?size == 0>
  No components.
  <#else>
  <ul>
  <#list components as component>
    <li><a href="${Root.path}/${distId}/viewComponent/${component.name}">${component.name}</a></li>
  </#list>
  </ul>
  </#if>

</div>

</@block>

</@extends>

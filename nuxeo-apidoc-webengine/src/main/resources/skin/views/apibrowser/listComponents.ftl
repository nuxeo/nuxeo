<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<#if searchFilter??>
  <h1> listing components with filter '${searchFilter}' </h1>
</#if>
<#if !searchFilter>
  <h1> listing all components</h1>
</#if>

  <#if !Root.currentDistribution.live>
    <span style="float:right">
    <form method="GET" action="${Root.path}/${distId}/filterComponents" >
      <input type="text" name="fulltext" value="${searchFilter}">
      <input type="submit" value="filter">
    </form>
    <#if searchFilter??>
      <A href="${Root.path}/${distId}/listComponents"> [ Reset ] </A>
    </#if>
    </span>
  </#if>


<h2> listing all deployed java components (${javaComponents?size})</h2>
<#list javaComponents as component>

  <A href="${Root.path}/${distId}/viewComponent/${component.id}">${component.label}</A><br/>

</#list>

<h2> listing all deployed pure XML components (${xmlComponents?size})</h2>
<#list xmlComponents as component>

  <A href="${Root.path}/${distId}/viewComponent/${component.id}">${component.label}</A><br/>

</#list>

</@block>

</@extends>
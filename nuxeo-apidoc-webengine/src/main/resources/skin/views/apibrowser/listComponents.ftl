<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h1> listing all deployed components</h1>

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
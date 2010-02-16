<@extends src="base.ftl">
<#setting url_escaping_charset="UTF-8">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.base.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.tabs.js"></script>
</@block>

<@block name="right">
<h1> view component ${component.name} </h1>

<#if component.xmlPureComponent>
Xml  Component
</#if>

<#if !component.xmlPureComponent>
Implementation class : ${component.componentClass}
</#if>
<br/>

<div class="tab">
<h2> Documentation </h2>
${component.documentation}

XML File:
<pre style="border-width:1px;border-style:solid;border-color:black">

 ${component.xmlFileContent?html}
</pre>

<h2> Services </h2>
<ul>
<#list component.serviceNames as service>
    <li><A href="${Root.path}/${distId}/viewService/${service}"> ${service} </A></li>
</#list>
</ul>

<h2> ExtensionPoints </h2>
<ul>
<#list component.extensionPoints as ep>
    <li><A href="${Root.path}/${distId}/viewExtensionPoint/${ep.name}"> ${ep.name} </A></li>
</#list>
</ul>

<h2> Contributions </h2>
<ul>
<#list component.extensions as ex>
    <li><A href="${Root.path}/${distId}/viewContribution/${ex.id?url}"> ${ex.id} </A></li>
</#list>

</ul>

</@block>

</@extends>
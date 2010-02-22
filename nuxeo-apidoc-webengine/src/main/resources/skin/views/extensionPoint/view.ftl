<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">
<h1> view ExtensionPoint ${extensionPoint.name} </h1>

<h2> Documentation </h2>
<pre>
${extensionPoint.documentation?html}
</pre>
<h2> Contributions </h2>
<ul>
<#list extensionPoint.extensions as contrib>
    <li>From <A href="${Root.path}/${distId}/viewComponent/${contrib.targetComponentName.name}"> ${contrib.targetComponentName.name}</A> contribution : <A href="${Root.path}/${distId}/viewContribution/${contrib.id}"> ${contrib.id} </A></li>
</#list>
</ul>

</@block>

</@extends>
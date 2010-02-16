<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">
<h1> view Bundle ${bundle.bundleId}</h1>

<h2> Filename </h2>
${bundle.fileName}

<h2> Maven Artifact</h2>
ArtifactId : ${bundle.artifactId} <br/>
GroupId : ${bundle.artifactGroupId} <br/>
Version : ${bundle.artifactVersion} <br/>

<h2> MANIFEST </h2>
<pre>
${bundle.manifest}
</pre>

<h2> Components </h2>
<ul>
<#list components as component>
    <li><A href="${Root.path}/${distId}/viewComponent/${component.name}"> ${component.name} </A></li>
</#list>
</ul>
</@block>

</@extends>
<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">
<h1> view Contribution ${contribution.id} </h1>

<pre style="border-width:1px;border-style:solid;border-color:black">

${contribution.xml?html}
</pre>

<h2> target ExtensionPoint </h2>
<A href="${Root.path}/${distId}/viewExtensionPoint/${contribution.extensionPoint}">
${contribution.targetComponentName.name}
 --
${contribution.extensionPoint}
</A>


</@block>

</@extends>
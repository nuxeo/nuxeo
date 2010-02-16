<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">
<h1> view Bundle group ${groupId} </h1>

<h2> Sub Groups </h2>
<ul>
<#list group.subGroups as group>
    <li>From <A href="${Root.path}/${distId}/viewBundleGroup/${group.key}"> ${group.name}</A></li>
</#list>
</ul>

<h2> Bundles </h2>
<ul>
<#list group.bundleIds as bundle>
    <li><A href="${Root.path}/${distId}/viewBundle/${bundle}"> ${bundle}</A></li>
</#list>
</ul>

</@block>

</@extends>
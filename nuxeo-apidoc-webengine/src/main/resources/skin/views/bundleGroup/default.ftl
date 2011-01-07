<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Bundle group ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1>Bundle group <span class="componentTitle">${nxItem.id}</span></h1>

<#include "/views/bundleGroup/bundleGroupMacros.ftl">

<@viewBundleGroup bundleGroupWO=This />

</@block>

</@extends>
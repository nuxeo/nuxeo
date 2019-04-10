<@extends src="base.ftl">

<@block name="stylesheets"></@block>
<@block name="header_scripts"></@block>

<@block name="right">
<#if mode=="create">
  <h1> Add Documentation for ${nxItem.id}</h1>
</#if>
<#if mode=="edit">
  <h1> Edit ${docItem.typeLabel} for ${nxItem.id}</h1>
</#if>

<#include "docItemForm.ftl">

</@block>

</@extends>
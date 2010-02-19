<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">
<#if mode=="create">
  <H1> Add Documentation for ${nxItem.id}</H1>
</#if>
<#if mode=="edit">
  <H1> Edit Documentation for ${nxItem.id}</H1>
</#if>

<#include "docItemForm.ftl">

</@block>

</@extends>
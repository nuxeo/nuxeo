<@extends src="base.ftl">
<@block name="title">Service ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Service <span class="componentTitle">${nxItem.id}</span></h1>
<div class="include-in components">In component <a href="${Root.path}/${distId}/viewComponent/${nxItem.componentId}">${nxItem.componentId}</a></div>

<div class="tabscontent">

  <h2>Implementation</h2>
  <div class="implementation">
    <@javadoc nxItem.id true />
  </div>

</div>

</@block>
</@extends>

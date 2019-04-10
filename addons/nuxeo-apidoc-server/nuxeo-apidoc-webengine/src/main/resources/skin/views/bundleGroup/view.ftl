<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">
<h1> view Bundle group ${groupId} </h1>

<div class="tabscontent">

  <h2> Sub Groups </h2>
  <ul>
  <#list group.subGroups as group>
      <li>From <a href="${Root.path}/${distId}/viewBundleGroup/${group.key}"> ${group.name}</a></li>
  </#list>
  </ul>

  <h2> Bundles </h2>
  <ul>
  <#list group.bundleIds as bundle>
      <li><a href="${Root.path}/${distId}/viewBundle/${bundle}"> ${bundle}</a></li>
  </#list>
  </ul>

</div>

</@block>

</@extends>
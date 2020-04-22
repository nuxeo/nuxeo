<@extends src="base.ftl">
<@block name="title">All bundle groups</@block>

<@block name="right">

<h1>All bundle groups (${tree?size})</h1>

<div class="tabscontent">

  <#assign prevLevel=-1/>

  <#list tree as bundleGroup>
    <#assign level=bundleGroup.level/>

    <#if level gt prevLevel>
      <#list prevLevel..level-1 as i>
      <ul>
      </#list>
    </#if>
    <#if level lt prevLevel>
      <#list level..prevLevel-1 as i>
      </ul>
      </#list>
    </#if>

    <li>
      <a href="${Root.path}/${distId}/viewBundleGroup/${bundleGroup.group.id}" class="itemLink">${bundleGroup.group.name}</a>
    </li>

    <#assign prevLevel=level/>
  </#list>

  <#list 0..prevLevel as i>
  </ul>
  </#list>
</div>

</@block>
</@extends>

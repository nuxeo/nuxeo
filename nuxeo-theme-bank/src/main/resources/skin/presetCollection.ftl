<@extends src="base.ftl">

  <@block name="title">
      ${collection}
  </@block>

  <@block name="content">
    <h1>${collection}</h1>
    <ul>
    <#list presets as preset>
      <li><a href="${Root.getPath()}/${bank}/preset/${collection}/${preset}">${preset}</a></li>
    </#list>
    </ul>
  </@block>

</@extends>

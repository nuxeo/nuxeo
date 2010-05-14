<@extends src="base.ftl">

  <@block name="title">
      ${collection}
  </@block>

  <@block name="content">
    <h1>${collection}</h1>
    <ul>
    <#list styles as style>
      <li><a href="${Root.getPath()}/${bank}/style/${collection}/${style}">${style}</a></li>
    </#list>
    </ul>
  </@block>

</@extends>

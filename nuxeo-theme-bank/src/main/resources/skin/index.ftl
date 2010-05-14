<@extends src="base.ftl">

  <@block name="title">
      <title>Nuxeo Theme Bank</title>
  </@block>

  <@block name="content">
    <h1>Nuxeo Theme Bank</h1>
    <ul>
    <#list Root.getBankNames() as bank>
      <li><a href="${Root.getPath()}/${bank}">${bank}</a></li>
    </#list>
    </ul>
  </@block>

</@extends>

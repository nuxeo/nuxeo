<@extends src="base.ftl">
  <@block name="title">Queue ${This.queueName}</@block>
  
  <@block name="header">
    Queue ${This.queueName}
  </@block>
  <@block name="content">
    List of queueItem:
    <ul>
      <#list This.listQueue as queueItem>
      <li><${queueItem}</a></li>
      </#list>
    </ul>

  </@block>
</@extends>

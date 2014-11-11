<@extends src="base.ftl">
  <@block name="title">Queues Management</@block>
  <@block name="content">
    List of available queues:
    <ul>
      <#list This.listQueues as queueName>
      <li><a href="${This.name}/${queueName}">${queueName}</a></li>
      </#list>
    </ul>

  </@block>
</@extends>

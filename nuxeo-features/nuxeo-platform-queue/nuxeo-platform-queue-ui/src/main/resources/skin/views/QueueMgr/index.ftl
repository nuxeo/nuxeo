<@extends src="base.ftl">
  <@block name="title">Queues Management</@block>
  
  <@block name="header">
  	Queues Management
  </@block>
  <@block name="content">
    List of available queues:
    <ul>
      <#list This.listQueues as queueName>
      <li><a href="${queueName}">${queueName}</a></li>
      </#list>
    </ul>

  </@block>
</@extends>

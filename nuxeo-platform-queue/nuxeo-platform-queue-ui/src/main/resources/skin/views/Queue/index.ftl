<@extends src="base.ftl">
  <@block name="title">Queue ${This.queueName}</@block>
  <@block name="content">
    List of queueItem:
    <table>
      <tr> <th>relaunch</th><th>Content Name</th>  <th>Owner</th><th>Orphaned</th><th>Comments</th></tr>
      <#list This.queueItems as item>
      <tr> <td><a href="${This.name}/${item.handledContent.name}/start">relaunch</a></td><td>${item.handledContent.name}</td><td>${item.handledContent.owner}</td><td>${item.orphaned}</td><td>${item.handledContent.comments}</td>    </tr>
      </#list>
    </table>
    </ul>

  </@block>
</@extends>

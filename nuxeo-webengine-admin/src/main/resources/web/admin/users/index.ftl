<@extends src="base.ftl">

<@block name="header"><h1><a href ="${This.previous.path}">User Administration</a></h1></@block>

<@block name="content">
<div id="entry-actions">
  <div class="ui-tabs-panel">

    <h1>Search</h1>

    <div class="userSearch">
      <h2>Users</h2>
      <form method="GET" action="${Context.URL}" accept-charset="utf-8">
        <input type="text" name="query" value=""/>
        <input type="submit" value="Search"/>
      </form>
    </div>

    <div class="groupSearch">
      <h2>Groups</h2>
      <form method="GET" action="${Context.URL}" accept-charset="utf-8">
        <input type="text" name="query" value=""/>
        <input type="hidden" name="group" value="true"/>
        <input type="submit" value="Search"/>
      </form>
    </div>

    <#if users>
      <table class="itemListing directories">
      <thead>
      	<tr>
      		<th>User Name</th>
      		<th>First Name</th>
      		<th>Last Name</th>
      	</tr>
      </thead>
      <tbody>
      <#list users as user>
      	<tr>
      		<td><a href="${This.path}/user/${user}">${user}<a/></td>
      		<td>${user.firstName}</td>
      		<td>${user.lastName}</td>
      	</tr>
      </#list>
      </tbody>
      </table>
    </#if>

    <#if groups>
      <table class="itemListing directories">
      <thead>
        <tr>
          <th>Group Name</th>
        </tr>
      </thead>
      <tbody>
      <#list groups as user>
        <tr>
          <td><a href="${This.path}/group/${user}">${user}<a/></td>
        </tr>
      </#list>
      </tbody>
      </table>
    </#if>

  </div>
</div>
</@block>
</@extends>
